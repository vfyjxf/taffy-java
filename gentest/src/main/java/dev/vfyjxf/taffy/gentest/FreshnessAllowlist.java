package dev.vfyjxf.taffy.gentest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Explicit, metadata-carrying exceptions to the generated-tree byte comparison.
 *
 * <p>Each non-comment line has five {@code |}-separated fields:</p>
 *
 * <pre>
 * &lt;relative path under the generated root&gt; | &lt;@DisplayName or method name&gt; |
 * reason=&lt;why the committed expectation is kept&gt; |
 * since=&lt;when/where it was introduced&gt; |
 * removeBy=&lt;when the entry must be deleted&gt;
 * </pre>
 *
 * <p>The allowlist is intentionally narrow: it names individual generated test
 * methods, never whole files. The freshness check reports every skipped method
 * and fails when an entry no longer suppresses a real difference (stale
 * entry), so the file cannot silently grow into a blanket exclusion.</p>
 */
final class FreshnessAllowlist {

    /** One allowlisted hand edit. */
    record Entry(Path relativePath, String method, String reason, String since, String removeBy) {}

    private FreshnessAllowlist() {}

    static List<Entry> read(Path file) throws IOException {
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (NoSuchFileException ex) {
            throw new GeneratorOptions.UsageException("Freshness allowlist file does not exist: " + file);
        }
        List<Entry> entries = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i).trim();
            if (raw.isEmpty() || raw.startsWith("#")) {
                continue;
            }
            Entry entry = parseLine(raw, file, i + 1);
            String key = entry.relativePath() + "::" + entry.method();
            if (!seen.add(key)) {
                throw new GeneratorOptions.UsageException(
                        "Duplicate freshness allowlist entry " + key + " at " + file + ":" + (i + 1));
            }
            entries.add(entry);
        }
        return List.copyOf(entries);
    }

    private static Entry parseLine(String line, Path file, int lineNumber) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 5) {
            throw invalid(file, lineNumber,
                    "expected 5 '|' separated fields: <path> | <method> | reason=... | since=... | removeBy=...");
        }
        Path relative = parseRelativePath(parts[0].trim(), file, lineNumber);
        String method = parts[1].trim();
        if (method.isEmpty()) {
            throw invalid(file, lineNumber, "the method/@DisplayName field is empty");
        }
        String reason = parseField(parts[2], "reason", file, lineNumber);
        String since = parseField(parts[3], "since", file, lineNumber);
        String removeBy = parseField(parts[4], "removeBy", file, lineNumber);
        return new Entry(relative, method, reason, since, removeBy);
    }

    private static Path parseRelativePath(String raw, Path file, int lineNumber) {
        if (raw.isEmpty()) {
            throw invalid(file, lineNumber, "the relative path field is empty");
        }
        Path path;
        try {
            path = Path.of(raw);
        } catch (RuntimeException ex) {
            throw invalid(file, lineNumber, "invalid relative path '" + raw + "': " + ex.getMessage());
        }
        if (path.isAbsolute()) {
            throw invalid(file, lineNumber, "the path must be relative to the generated root: " + raw);
        }
        for (Path part : path) {
            if (part.toString().equals("..") || part.toString().equals(".")) {
                throw invalid(file, lineNumber, "the path must not contain '.' or '..': " + raw);
            }
        }
        return path.normalize();
    }

    private static String parseField(String raw, String name, Path file, int lineNumber) {
        String trimmed = raw.trim();
        String prefix = name + "=";
        if (!trimmed.startsWith(prefix)) {
            throw invalid(file, lineNumber, "expected field '" + prefix + "...' (got '" + trimmed + "')");
        }
        String value = trimmed.substring(prefix.length()).trim();
        if (value.isEmpty()) {
            throw invalid(file, lineNumber, "field '" + name + "' is empty");
        }
        return value;
    }

    private static GeneratorOptions.UsageException invalid(Path file, int lineNumber, String message) {
        return new GeneratorOptions.UsageException(
                "Invalid freshness allowlist entry at " + file + ":" + lineNumber + ": " + message);
    }
}
