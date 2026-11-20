package dev.vfyjxf.taffy.gentest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parser/validation coverage for the freshness allowlist metadata. */
class FreshnessAllowlistTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesCommentsBlanksAndFiveFieldEntries() throws Exception {
        Path file = writeAllowlist("""
                # ignored
                flex/FlexTest.java | align_a__border_box | reason=BK-13 rounding | since=2026-11-19 a9490f8 | removeBy=regenerate after #1189

                flex/FlexTest.java | align_a__content_box | reason=BK-13 rounding | since=2026-11-19 a9490f8 | removeBy=regenerate after #1189
                """);

        List<FreshnessAllowlist.Entry> entries = FreshnessAllowlist.read(file);

        assertEquals(2, entries.size());
        FreshnessAllowlist.Entry first = entries.get(0);
        assertEquals(Path.of("flex", "FlexTest.java"), first.relativePath());
        assertEquals("align_a__border_box", first.method());
        assertEquals("BK-13 rounding", first.reason());
        assertEquals("2026-11-19 a9490f8", first.since());
        assertEquals("regenerate after #1189", first.removeBy());
    }

    @Test
    void rejectsLineWithoutFiveFields() throws Exception {
        Path file = writeAllowlist(
                "flex/FlexTest.java | align_a | reason=x | since=y");

        GeneratorOptions.UsageException error = assertThrows(GeneratorOptions.UsageException.class,
                () -> FreshnessAllowlist.read(file));

        assertTrue(error.getMessage().contains("expected 5 '|' separated fields"), error.getMessage());
        assertTrue(error.getMessage().contains(file + ":1"), error.getMessage());
    }

    @Test
    void rejectsMissingReasonField() throws Exception {
        Path file = writeAllowlist(
                "flex/FlexTest.java | align_a | because | since=y | removeBy=z");

        GeneratorOptions.UsageException error = assertThrows(GeneratorOptions.UsageException.class,
                () -> FreshnessAllowlist.read(file));

        assertTrue(error.getMessage().contains("expected field 'reason="), error.getMessage());
    }

    @Test
    void rejectsAbsoluteAndParentTraversalPaths() throws Exception {
        Path absolute = writeAllowlist(
                "/tmp/FlexTest.java | align_a | reason=x | since=y | removeBy=z");
        Path traversal = writeAllowlist(
                "../FlexTest.java | align_a | reason=x | since=y | removeBy=z");

        GeneratorOptions.UsageException absoluteError = assertThrows(GeneratorOptions.UsageException.class,
                () -> FreshnessAllowlist.read(absolute));
        assertTrue(absoluteError.getMessage().contains("must be relative"), absoluteError.getMessage());

        GeneratorOptions.UsageException traversalError = assertThrows(GeneratorOptions.UsageException.class,
                () -> FreshnessAllowlist.read(traversal));
        assertTrue(traversalError.getMessage().contains("must not contain '.' or '..'"),
                traversalError.getMessage());
    }

    @Test
    void rejectsDuplicateEntries() throws Exception {
        String entry = "flex/FlexTest.java | align_a | reason=x | since=y | removeBy=z";
        Path file = writeAllowlist(entry + "\n" + entry + "\n");

        GeneratorOptions.UsageException error = assertThrows(GeneratorOptions.UsageException.class,
                () -> FreshnessAllowlist.read(file));

        assertTrue(error.getMessage().contains("Duplicate freshness allowlist entry"), error.getMessage());
    }

    private Path writeAllowlist(String content) throws Exception {
        Path file = Files.createTempFile(tempDir, "allowlist", ".txt");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }
}
