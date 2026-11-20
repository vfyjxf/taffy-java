package dev.vfyjxf.taffy.gentest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Deterministic HTML fixture discovery and filtering.
 *
 * <p>Filters are applied to the root-relative path (always using {@code /}) and
 * are evaluated in the order: category, filter file, filter globs. Results are
 * sorted by category then relative path so repeated runs and {@code --check}
 * are stable across filesystems/JVMs.</p>
 */
final class FixtureDiscovery {

    private FixtureDiscovery() {}

    record Fixture(Path root, Path path, String relativePath, String category, String name) {}

    static List<Fixture> discoverTaffy(Path fixturesRoot, GeneratorOptions options) throws IOException {
        List<String> filterFile = readFilterFile(options.filterFile);
        List<Fixture> out = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(fixturesRoot)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".html"))
                    // Historical behavior: fixtures whose name starts with "x" are disabled.
                    .filter(p -> !p.getFileName().toString().startsWith("x"))
                    .sorted()
                    .toList();
            for (Path file : files) {
                String rel = normalize(fixturesRoot.relativize(file).toString());
                Path parent = file.getParent();
                String category = parent == null ? "" : normalize(fixturesRoot.relativize(parent).toString());
                if (category.isEmpty()) {
                    category = "root";
                }
                String name = file.getFileName().toString().replaceFirst("\\.html$", "");
                if (!matches(fixturesRoot, rel, name, category, options, filterFile)) {
                    continue;
                }
                out.add(new Fixture(fixturesRoot, file, rel, category, name));
            }
        }
        out.sort(Comparator.comparing(Fixture::category).thenComparing(Fixture::relativePath));
        return out;
    }

    static List<Fixture> discoverWpt(Path cssRoot, List<String> suites, int maxFiles, GeneratorOptions options) throws IOException {
        List<String> filterFile = readFilterFile(options.filterFile);
        List<String> discovered = new ArrayList<>();
        for (String suite : suites) {
            String suiteRel = normalize(suite);
            while (suiteRel.startsWith("/")) {
                suiteRel = suiteRel.substring(1);
            }
            Path suiteDir = cssRoot.resolve(suiteRel).normalize();
            if (!Files.isDirectory(suiteDir)) {
                System.err.println("[warn] suite dir not found: " + suiteDir);
                continue;
            }
            try (Stream<Path> stream = Files.walk(suiteDir)) {
                List<Path> files = stream.filter(Files::isRegularFile).sorted().toList();
                for (Path file : files) {
                    if (maxFiles > 0 && discovered.size() >= maxFiles) {
                        break;
                    }
                    String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (!(fileName.endsWith(".html") || fileName.endsWith(".htm")
                            || fileName.endsWith(".xhtml") || fileName.endsWith(".xht"))) {
                        continue;
                    }
                    if (fileName.endsWith("-ref.html") || fileName.endsWith("-notref.html")
                            || fileName.contains("-manual")) {
                        continue;
                    }
                    String rel = normalize(cssRoot.relativize(file.toAbsolutePath().normalize()).toString());
                    if (rel.contains("/support/") || rel.contains("/_support/")
                            || rel.contains("/reference/") || rel.contains("/resources/")) {
                        continue;
                    }
                    if (!matches(cssRoot, rel, file.getFileName().toString(), suiteRel, options, filterFile)) {
                        continue;
                    }
                    discovered.add(rel);
                }
            }
            if (maxFiles > 0 && discovered.size() >= maxFiles) {
                break;
            }
        }
        discovered = discovered.stream().distinct().sorted().toList();

        List<Fixture> out = new ArrayList<>(discovered.size());
        for (String rel : discovered) {
            Path file = cssRoot.resolve(rel).normalize();
            String name = wptTestName(rel);
            out.add(new Fixture(cssRoot, file, rel, suiteFromPath(rel), name));
        }
        return out;
    }

    static String suiteFromPath(String rel) {
        String p = normalize(rel);
        if (p.startsWith("css/css-grid/")) {
            return "grid";
        }
        if (p.startsWith("css/css-flexbox/")) {
            return "flexbox";
        }
        if (p.startsWith("css/css-sizing/aspect-ratio/")) {
            return "sizing_aspect_ratio";
        }
        return null;
    }

    static String wptTestName(String rel) {
        String relNorm = normalize(rel);
        String prefix;
        if (relNorm.startsWith("css/css-grid/")) {
            prefix = relNorm.substring("css/css-grid/".length());
        } else if (relNorm.startsWith("css/css-flexbox/")) {
            prefix = relNorm.substring("css/css-flexbox/".length());
        } else if (relNorm.startsWith("css/css-sizing/aspect-ratio/")) {
            prefix = relNorm.substring("css/css-sizing/aspect-ratio/".length());
        } else {
            prefix = relNorm;
        }
        prefix = prefix.replaceAll("\\.(html|htm|xhtml|xht)$", "");
        String[] parts = prefix.split("/");
        String name = parts.length >= 2
                ? parts[parts.length - 2] + "_" + parts[parts.length - 1]
                : prefix;
        return name.replaceAll("[^a-zA-Z0-9_]+", "_");
    }

    private static boolean matches(Path root, String rel, String name, String category,
                                   GeneratorOptions options, List<String> filterFile) {
        if (!options.categories.isEmpty() && !options.categories.contains(category)) {
            return false;
        }
        if (!filterFile.isEmpty() && !filterFile.contains(rel)) {
            return false;
        }
        if (options.filters.isEmpty()) {
            return true;
        }
        for (String glob : options.filters) {
            if (globMatches(glob, rel) || globMatches(glob, name)) {
                return true;
            }
        }
        return false;
    }

    static boolean globMatches(String glob, String value) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append('.');
                case '.', '(', ')', '+', '|', '^', '$', '@', '%', '[', ']', '{', '}' -> regex.append('\\').append(c);
                case '\\' -> regex.append("\\\\");
                default -> regex.append(c);
            }
        }
        return value.matches(regex.toString());
    }

    private static List<String> readFilterFile(Path filterFile) throws IOException {
        if (filterFile == null) {
            return List.of();
        }
        if (!Files.isRegularFile(filterFile)) {
            throw new GeneratorOptions.UsageException("--filter-file is not a file: " + filterFile);
        }
        Set<String> paths = new LinkedHashSet<>();
        for (String line : Files.readAllLines(filterFile, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                paths.add(normalize(trimmed));
            }
        }
        return List.copyOf(paths);
    }

    static String normalize(String path) {
        return path.replace('\\', '/');
    }
}
