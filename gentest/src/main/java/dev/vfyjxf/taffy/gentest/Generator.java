package dev.vfyjxf.taffy.gentest;

import dev.vfyjxf.taffy.gentest.FixtureDiscovery.Fixture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Unified entry point for taffy fixture and WPT test generation.
 */
public final class Generator {

    private Generator() {}

    public static void main(String[] args) {
        int code = run(args);
        if (code != 0) {
            System.exit(code);
        }
    }

    static int run(String[] args) {
        GeneratorOptions options;
        try {
            options = GeneratorOptions.parse(args);
        } catch (GeneratorOptions.UsageException ex) {
            System.err.println("error: " + ex.getMessage());
            return 2;
        }

        if (options.help) {
            System.out.println(GeneratorOptions.usage());
            return 0;
        }

        try {
            if (options.convertToFixtures) {
                return runConversion(options);
            }
            if (options.source == GeneratorOptions.Source.ALL) {
                int first = runOne(options, true);
                if (first != 0) {
                    return first;
                }
                return runOne(options, false);
            }
            return runOne(options, options.source == GeneratorOptions.Source.TAFFY);
        } catch (GeneratorOptions.UsageException ex) {
            System.err.println("error: " + ex.getMessage());
            return 2;
        } catch (Exception ex) {
            System.err.println("error: " + ex.getMessage());
            ex.printStackTrace();
            return 1;
        }
    }

    private static int runOne(GeneratorOptions options, boolean taffy) throws IOException {
        if (options.dryRun) {
            dryRun(options, taffy);
            return 0;
        }

        // Read (and validate) the allowlist before paying the Chrome startup cost.
        // The allowlist is taffy-only: WPT output is not committed and its paths
        // would otherwise look like unmatched entries.
        List<FreshnessAllowlist.Entry> freshnessAllowlist = taffy && options.freshnessAllowlist != null
                ? FreshnessAllowlist.read(options.freshnessAllowlist)
                : List.of();

        Path outputRoot = options.outRoot;
        Path temporaryRoot = null;
        if (options.check) {
            temporaryRoot = Files.createTempDirectory("taffy-gentest-check-");
            outputRoot = temporaryRoot;
            System.out.println("Check mode: generating into " + outputRoot);
        }

        try {
            if (taffy) {
                Path packageRoot = outputRoot.resolve(packagePath(options.taffyPackage()));
                // Validate paths/fixtures before paying the Chrome startup cost.
                TaffySource.resolveFixturesRoot(options);
                TaffySource.discover(options);
                if (!options.check && options.categories.isEmpty() && options.filters.isEmpty()
                        && options.filterFile == null) {
                    // Historical behavior: a full taffy run owns the generated package.
                    JavaTestWriter.deleteDirectory(packageRoot);
                    // Remove the pre-rename package once, as the old generator did.
                    Path legacy = outputRoot.resolve("com/nicecoder/taffy/generated");
                    if (!legacy.equals(packageRoot)) {
                        JavaTestWriter.deleteDirectory(legacy);
                    }
                }
                try (ChromeOracle oracle = ChromeOracle.start(options.chromeBinary, options.chromeDriver)) {
                    TaffySource.generate(options, packageRoot, oracle);
                }
                if (options.check) {
                    Path expected = options.outRoot.resolve(packagePath(options.taffyPackage()));
                    return compareTrees(expected, packageRoot,
                            "./gradlew :gentest:genTaffy -PtaffyRoot=<taffy checkout>",
                            freshnessAllowlist) ? 0 : 1;
                }
            } else {
                Path packageRoot = outputRoot.resolve(packagePath(options.wptPackage()));
                // Validate paths/fixtures before paying the Chrome startup cost.
                WptSource.resolveCssRoot(options);
                WptSource.discover(options);
                try (ChromeOracle oracle = ChromeOracle.start(
                        options.chromeBinary, options.chromeDriver,
                        CssValues.VIEWPORT_WIDTH, CssValues.VIEWPORT_HEIGHT)) {
                    WptSource.generate(options, packageRoot, oracle);
                }
                if (options.check) {
                    Path expected = options.outRoot.resolve(packagePath(options.wptPackage()));
                    return compareTrees(expected, packageRoot,
                            "./gradlew :gentest:genWpt -PwptRoot=<wpt checkout>",
                            List.of()) ? 0 : 1;
                }
            }
        } finally {
            if (temporaryRoot != null) {
                try {
                    JavaTestWriter.deleteDirectory(temporaryRoot);
                } catch (IOException ex) {
                    System.err.println("[warn] could not delete temp dir " + temporaryRoot + ": " + ex.getMessage());
                }
            }
        }
        return 0;
    }

    private static void dryRun(GeneratorOptions options, boolean taffy) throws IOException {
        if (taffy) {
            List<Fixture> fixtures = TaffySource.discover(options);
            Map<String, List<Fixture>> byCategory = new TreeMap<>();
            for (Fixture fixture : fixtures) {
                byCategory.computeIfAbsent(fixture.category(), key -> new ArrayList<>()).add(fixture);
            }
            Path packageRoot = options.outRoot.resolve(packagePath(options.taffyPackage()));
            System.out.println("[dry-run] taffy fixtures: " + fixtures.size());
            for (Map.Entry<String, List<Fixture>> entry : byCategory.entrySet()) {
                Path file = packageRoot.resolve(entry.getKey())
                        .resolve(StyleCodegen.toPascalCase(entry.getKey()) + "Test.java");
                System.out.println("[dry-run] would generate " + file + " (" + entry.getValue().size()
                        + " fixtures, ~" + (entry.getValue().size() * 2) + " tests)");
            }
        } else {
            List<Fixture> fixtures = WptSource.discover(options);
            Map<String, List<Fixture>> bySuite = new TreeMap<>();
            for (Fixture fixture : fixtures) {
                bySuite.computeIfAbsent(fixture.category(), key -> new ArrayList<>()).add(fixture);
            }
            Path packageRoot = options.outRoot.resolve(packagePath(options.wptPackage()));
            System.out.println("[dry-run] WPT tests: " + fixtures.size());
            for (Map.Entry<String, List<Fixture>> entry : bySuite.entrySet()) {
                int count = entry.getValue().size();
                int chunks = (count + WptSource.MAX_TESTS_PER_CLASS - 1) / WptSource.MAX_TESTS_PER_CLASS;
                for (int chunk = 1; chunk <= chunks; chunk++) {
                    String base = JavaTestWriter.wptSuiteClass(entry.getKey());
                    String className = count <= WptSource.MAX_TESTS_PER_CLASS ? base : base + "_" + chunk;
                    Path file = packageRoot.resolve(entry.getKey()).resolve(className + ".java");
                    int from = (chunk - 1) * WptSource.MAX_TESTS_PER_CLASS;
                    int to = Math.min(count, from + WptSource.MAX_TESTS_PER_CLASS);
                    System.out.println("[dry-run] would generate " + file + " (" + (to - from) + " tests)");
                }
            }
        }
    }

    private static int runConversion(GeneratorOptions options) throws IOException {
        if (options.dryRun) {
            WptToFixtureConverter.convert(options.convertInput, options.convertOutput,
                    options.convertPrefix, options.convertPattern, options.convertRecursive, true);
            return 0;
        }
        WptToFixtureConverter.convert(options.convertInput, options.convertOutput,
                options.convertPrefix, options.convertPattern, options.convertRecursive, false);
        return 0;
    }

    static Path packagePath(String packageName) {
        return Path.of(packageName.replace('.', java.io.File.separatorChar));
    }

    /**
     * Byte-compare the generated tree against the committed tree.
     *
     * <p>Normally every file must be byte-identical. Files listed in
     * {@code allowlist} are compared method by method instead: the named
     * generated methods are skipped and reported, while every other byte must
     * still match. An allowlisted method that no longer differs is reported as
     * stale and fails the check, so an exception cannot silently keep
     * validating nothing.</p>
     *
     * @return true when every generated file exists and matches, apart from the
     *         explicitly allowlisted hand edits
     */
    static boolean compareTrees(Path expectedRoot, Path actualRoot, String refreshCommand,
                                List<FreshnessAllowlist.Entry> allowlist) throws IOException {
        Map<Path, List<FreshnessAllowlist.Entry>> allowlistByFile = new LinkedHashMap<>();
        for (FreshnessAllowlist.Entry entry : allowlist) {
            allowlistByFile.computeIfAbsent(entry.relativePath(), key -> new ArrayList<>()).add(entry);
        }
        Set<FreshnessAllowlist.Entry> visitedEntries = new LinkedHashSet<>();
        Set<String> refreshCategories = new TreeSet<>();

        List<Path> actualFiles = listJava(actualRoot);
        if (actualFiles.isEmpty()) {
            System.err.println("CHECK FAILED: no generated files under " + actualRoot);
            return false;
        }
        int compared = 0;
        int differences = 0;
        int allowedEdits = 0;
        Set<Path> actualRelative = new LinkedHashSet<>();
        Set<Path> touchedDirs = new LinkedHashSet<>();

        for (Path actual : actualFiles) {
            Path relative = actualRoot.relativize(actual);
            actualRelative.add(relative);
            Path parent = relative.getParent();
            if (parent != null) {
                touchedDirs.add(parent);
            }
            Path expected = expectedRoot.resolve(relative);
            if (!Files.isRegularFile(expected)) {
                System.err.println("CHECK FAILED: missing expected file " + expected);
                differences++;
                addCategory(refreshCategories, relative);
                continue;
            }
            compared++;
            List<FreshnessAllowlist.Entry> fileAllowlist = allowlistByFile.get(relative);
            if (fileAllowlist == null || fileAllowlist.isEmpty()) {
                byte[] expectedBytes = Files.readAllBytes(expected);
                byte[] actualBytes = Files.readAllBytes(actual);
                if (!Arrays.equals(expectedBytes, actualBytes)) {
                    differences++;
                    addCategory(refreshCategories, relative);
                    System.err.println("CHECK FAILED: differs: " + expected);
                    printDiff(expected, expectedBytes, actualBytes);
                }
                continue;
            }
            AllowlistedComparison comparison = compareAllowlistedFile(
                    expected, actual, relative, fileAllowlist, visitedEntries);
            if (comparison.passed()) {
                allowedEdits += comparison.allowedEdits();
            } else {
                differences++;
                addCategory(refreshCategories, relative);
            }
        }

        for (Path expected : listJava(expectedRoot)) {
            Path relative = expectedRoot.relativize(expected);
            Path parent = relative.getParent();
            if (parent != null && touchedDirs.contains(parent) && !actualRelative.contains(relative)) {
                System.err.println("CHECK FAILED: stale committed file not generated: " + expected);
                differences++;
                addCategory(refreshCategories, relative);
            }
        }

        for (FreshnessAllowlist.Entry entry : allowlist) {
            if (visitedEntries.contains(entry)) {
                continue;
            }
            // A category filter can leave an allowlisted file outside this run.
            // Report it, but only fail when the committed tree has no such file
            // at all (a typo or an entry left behind after a rename).
            Path committedFile = expectedRoot.resolve(entry.relativePath());
            if (Files.isRegularFile(committedFile)) {
                System.out.println("[info] freshness allowlist entry not evaluated "
                        + "(file outside this check): " + entry.relativePath()
                        + " :: " + entry.method());
                continue;
            }
            differences++;
            System.err.println("CHECK FAILED: allowlist entry matches no generated method: "
                    + entry.relativePath() + " :: " + entry.method()
                    + " [removeBy=" + entry.removeBy() + "]");
        }

        if (differences == 0) {
            String suffix = allowedEdits == 0 ? ""
                    : " (" + allowedEdits + " allowlisted hand edit(s) skipped)";
            System.out.println("CHECK PASSED: " + compared + " file(s) byte-identical under "
                    + expectedRoot + suffix);
            return true;
        }
        System.err.println("CHECK FAILED: " + differences + " difference(s) across " + compared
                + " compared file(s)");
        String categoryArg = refreshCategories.isEmpty()
                ? ""
                : " -Pcategory=" + String.join(",", refreshCategories);
        System.err.println("Refresh with: " + refreshCommand + categoryArg
                + " and commit the regenerated files (never edit generated tests by hand).");
        return false;
    }

    private record AllowlistedComparison(boolean passed, int allowedEdits) {}

    private record MethodBlock(String displayName, String methodName, int startLine, int endLine) {}

    private record NumberedLine(int number, String text) {}

    /**
     * Compare one file, skipping the method blocks named by the allowlist.
     * Every allowlisted method must suppress a real difference: an entry whose
     * block matches (or does not exist on one side) is stale and fails the check.
     */
    private static AllowlistedComparison compareAllowlistedFile(
            Path expected, Path actual, Path relative,
            List<FreshnessAllowlist.Entry> entries,
            Set<FreshnessAllowlist.Entry> visitedEntries) throws IOException {
        byte[] expectedBytes = Files.readAllBytes(expected);
        byte[] actualBytes = Files.readAllBytes(actual);
        if (containsCarriageReturn(expectedBytes) || containsCarriageReturn(actualBytes)) {
            System.err.println("CHECK FAILED: differs (CR bytes; allowlist comparison is line based): " + expected);
            printDiff(expected, expectedBytes, actualBytes);
            return new AllowlistedComparison(false, 0);
        }
        List<String> expectedLines = splitLines(expectedBytes);
        List<String> actualLines = splitLines(actualBytes);
        Map<String, MethodBlock> expectedBlocks = parseMethodBlocks(expectedLines);
        Map<String, MethodBlock> actualBlocks = parseMethodBlocks(actualLines);

        boolean failed = false;
        int allowed = 0;
        List<int[]> expectedSkips = new ArrayList<>();
        List<int[]> actualSkips = new ArrayList<>();
        for (FreshnessAllowlist.Entry entry : entries) {
            visitedEntries.add(entry);
            MethodBlock expectedBlock = expectedBlocks.get(entry.method());
            MethodBlock actualBlock = actualBlocks.get(entry.method());
            if (expectedBlock == null || actualBlock == null) {
                failed = true;
                System.err.println("CHECK FAILED: allowlist entry matches no test method on one side: "
                        + relative + " :: " + entry.method()
                        + " (expected=" + (expectedBlock != null)
                        + ", actual=" + (actualBlock != null)
                        + ", removeBy=" + entry.removeBy() + ")");
                continue;
            }
            String expectedBlockText = blockText(expectedLines, expectedBlock);
            String actualBlockText = blockText(actualLines, actualBlock);
            if (expectedBlockText.equals(actualBlockText)) {
                failed = true;
                System.err.println("CHECK FAILED: stale allowlist entry (regeneration already matches): "
                        + relative + " :: " + entry.method()
                        + " [since=" + entry.since() + ", removeBy=" + entry.removeBy() + "]");
                continue;
            }
            expectedSkips.add(new int[]{expectedBlock.startLine(), expectedBlock.endLine()});
            actualSkips.add(new int[]{actualBlock.startLine(), actualBlock.endLine()});
            allowed++;
            System.out.println("allowlisted hand edit skipped: " + relative + " :: " + entry.method()
                    + " (" + entry.reason() + ")");
        }

        boolean matches = printNumberedDiff(
                expected,
                remainingLines(expectedLines, expectedSkips),
                remainingLines(actualLines, actualSkips));
        if (!matches || failed) {
            return new AllowlistedComparison(false, 0);
        }
        return new AllowlistedComparison(true, allowed);
    }

    private static void addCategory(Set<String> categories, Path relative) {
        if (relative.getNameCount() > 1) {
            categories.add(relative.getName(0).toString());
        }
    }

    private static boolean containsCarriageReturn(byte[] bytes) {
        for (byte value : bytes) {
            if (value == '\r') {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitLines(byte[] bytes) {
        return Arrays.asList(new String(bytes, StandardCharsets.UTF_8).split("\n", -1));
    }

    private static String blockText(List<String> lines, MethodBlock block) {
        return String.join("\n", lines.subList(block.startLine(), block.endLine()));
    }

    private static Map<String, MethodBlock> parseMethodBlocks(List<String> lines) {
        Map<String, MethodBlock> blocks = new LinkedHashMap<>();
        int classClosingBrace = -1;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).equals("}")) {
                classClosingBrace = i;
                break;
            }
        }
        int start = -1;
        String displayName = null;
        String methodName = null;
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).equals("    @Test")) {
                continue;
            }
            if (start >= 0 && methodName != null) {
                addMethodBlock(blocks, displayName, methodName, start, i);
            }
            start = i;
            displayName = null;
            methodName = null;
            for (int j = i + 1; j < Math.min(i + 5, lines.size()); j++) {
                String line = lines.get(j).trim();
                if (displayName == null && line.startsWith("@DisplayName(")) {
                    int prefix = line.indexOf("(\"");
                    int suffix = line.lastIndexOf("\")");
                    if (prefix >= 0 && suffix > prefix + 2) {
                        displayName = line.substring(prefix + 2, suffix);
                    }
                }
                if (methodName == null && line.startsWith("void ")) {
                    int paren = line.indexOf('(', 5);
                    if (paren > 5) {
                        methodName = line.substring(5, paren).trim();
                    }
                }
            }
        }
        if (start >= 0 && methodName != null) {
            int end = classClosingBrace > start ? classClosingBrace : lines.size();
            addMethodBlock(blocks, displayName, methodName, start, end);
        }
        return blocks;
    }

    private static void addMethodBlock(Map<String, MethodBlock> blocks, String displayName,
                                       String methodName, int start, int end) {
        MethodBlock block = new MethodBlock(displayName, methodName, start, end);
        blocks.put(methodName, block);
        if (displayName != null) {
            blocks.putIfAbsent(displayName, block);
        }
    }

    private static List<NumberedLine> remainingLines(List<String> lines, List<int[]> skips) {
        List<NumberedLine> remaining = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            boolean skipped = false;
            for (int[] skip : skips) {
                if (i >= skip[0] && i < skip[1]) {
                    skipped = true;
                    break;
                }
            }
            if (!skipped) {
                remaining.add(new NumberedLine(i + 1, lines.get(i)));
            }
        }
        return remaining;
    }

    /**
     * Print the first differences between two numbered line sequences.
     *
     * @return true when the sequences are equal
     */
    private static boolean printNumberedDiff(Path expected, List<NumberedLine> expectedLines,
                                             List<NumberedLine> actualLines) {
        List<String[]> differences = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < expectedLines.size() || j < actualLines.size()) {
            NumberedLine left = i < expectedLines.size() ? expectedLines.get(i) : null;
            NumberedLine right = j < actualLines.size() ? actualLines.get(j) : null;
            if (left != null && right != null && left.text().equals(right.text())) {
                i++;
                j++;
                continue;
            }
            if (differences.size() < 20) {
                differences.add(new String[]{
                        left == null ? "<eof>" : Integer.toString(left.number()),
                        left == null ? "<missing>" : left.text(),
                        right == null ? "<eof>" : Integer.toString(right.number()),
                        right == null ? "<missing>" : right.text()});
            }
            if (left != null) {
                i++;
            }
            if (right != null) {
                j++;
            }
        }
        if (differences.isEmpty()) {
            return true;
        }
        System.err.println("CHECK FAILED: differs (after allowlisted methods): " + expected);
        for (String[] difference : differences) {
            System.err.println("  line " + difference[0] + " expected: " + difference[1]);
            System.err.println("  line " + difference[2] + " actual  : " + difference[3]);
        }
        return false;
    }

    private static List<Path> listJava(Path root) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private static void printDiff(Path expected, byte[] expectedBytes, byte[] actualBytes) {
        String[] expectedLines = new String(expectedBytes, StandardCharsets.UTF_8).split("\n", -1);
        String[] actualLines = new String(actualBytes, StandardCharsets.UTF_8).split("\n", -1);
        int shown = 0;
        int max = Math.max(expectedLines.length, actualLines.length);
        for (int i = 0; i < max && shown < 20; i++) {
            String left = i < expectedLines.length ? expectedLines[i] : "<missing>";
            String right = i < actualLines.length ? actualLines[i] : "<missing>";
            if (!left.equals(right)) {
                System.err.println("  line " + (i + 1) + " expected: " + left);
                System.err.println("  line " + (i + 1) + " actual  : " + right);
                shown++;
            }
        }
        if (shown == 0) {
            System.err.println("  files differ in trailing bytes/line endings");
        }
    }
}
