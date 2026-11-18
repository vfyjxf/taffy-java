package dev.vfyjxf.taffy.gentest;

import dev.vfyjxf.taffy.gentest.FixtureDiscovery.Fixture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
                    return compareTrees(expected, packageRoot) ? 0 : 1;
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
                    return compareTrees(expected, packageRoot) ? 0 : 1;
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
     * @return true when every generated file exists and is byte-identical
     */
    static boolean compareTrees(Path expectedRoot, Path actualRoot) throws IOException {
        List<Path> actualFiles = listJava(actualRoot);
        if (actualFiles.isEmpty()) {
            System.err.println("CHECK FAILED: no generated files under " + actualRoot);
            return false;
        }
        int compared = 0;
        int differences = 0;
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
                continue;
            }
            byte[] expectedBytes = Files.readAllBytes(expected);
            byte[] actualBytes = Files.readAllBytes(actual);
            compared++;
            if (!Arrays.equals(expectedBytes, actualBytes)) {
                differences++;
                System.err.println("CHECK FAILED: differs: " + expected);
                printDiff(expected, expectedBytes, actualBytes);
            }
        }

        for (Path expected : listJava(expectedRoot)) {
            Path relative = expectedRoot.relativize(expected);
            Path parent = relative.getParent();
            if (parent != null && touchedDirs.contains(parent) && !actualRelative.contains(relative)) {
                System.err.println("CHECK FAILED: stale committed file not generated: " + expected);
                differences++;
            }
        }

        if (differences == 0) {
            System.out.println("CHECK PASSED: " + compared + " file(s) byte-identical under " + expectedRoot);
            return true;
        }
        System.err.println("CHECK FAILED: " + differences + " difference(s) across " + compared + " compared file(s)");
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
