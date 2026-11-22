package dev.vfyjxf.taffy.gentest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure JUnit coverage for the byte-level generated-tree comparison used by
 * {@code :gentest:checkGenerated} / {@code :gentest:checkGeneratedFreshness}.
 * No Chrome or taffy checkout is required: every case builds two temp trees.
 */
class GeneratedTreeFreshnessTest {

    private static final String REFRESH_COMMAND = "./gradlew :gentest:genTaffy -PtaffyRoot=<taffy checkout>";

    @TempDir
    Path tempDir;

    @Test
    void identicalTreesPassAndIgnoreNonJavaFiles() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        write(expected, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);"),
                method("fixture_b", "fixtureB", "assertEquals(2.0f, 2.0f);")));
        write(expected, "README.md", "not java\n");
        write(actual, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);"),
                method("fixture_b", "fixtureB", "assertEquals(2.0f, 2.0f);")));

        Output output = compare(expected, actual, List.of());

        assertTrue(output.result(), output.err());
        assertEquals("", output.err());
        assertTrue(output.out().contains("CHECK PASSED: 1 file(s) byte-identical"), output.out());
    }

    @Test
    void changedByteFailsWithFileAndFirstDifference() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        write(expected, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);")));
        write(actual, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(2.0f, 2.0f);")));

        Output output = compare(expected, actual, List.of());

        assertFalse(output.result(), "a single changed byte must fail the check");
        String expectedFile = expected.resolve(Path.of("flex", "FlexTest.java")).toString();
        assertTrue(output.err().contains("CHECK FAILED: differs: " + expectedFile), output.err());
        assertTrue(output.err().contains("expected:         assertEquals(1.0f, 1.0f);"), output.err());
        assertTrue(output.err().contains("actual  :         assertEquals(2.0f, 2.0f);"), output.err());
        assertTrue(output.err().contains("Refresh with: " + REFRESH_COMMAND + " -Pcategory=flex"), output.err());
    }

    @Test
    void missingCommittedFileFails() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        write(expected, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);")));
        write(actual, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);")));
        write(actual, "grid/GridTest.java", generatedFile(
                method("fixture_g", "fixtureG", "assertEquals(3.0f, 3.0f);")));

        Output output = compare(expected, actual, List.of());

        assertFalse(output.result());
        assertTrue(output.err().contains("missing expected file "
                + expected.resolve(Path.of("grid", "GridTest.java"))), output.err());
    }

    @Test
    void staleCommittedFileFails() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        write(expected, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);")));
        write(expected, "flex/RemovedTest.java", generatedFile(
                method("fixture_old", "fixtureOld", "assertEquals(4.0f, 4.0f);")));
        write(actual, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);")));

        Output output = compare(expected, actual, List.of());

        assertFalse(output.result());
        assertTrue(output.err().contains("stale committed file not generated: "
                + expected.resolve(Path.of("flex", "RemovedTest.java"))), output.err());
    }

    @Test
    void emptyGeneratedTreeFails() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        write(expected, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);")));
        Files.createDirectories(actual);

        Output output = compare(expected, actual, List.of());

        assertFalse(output.result());
        assertTrue(output.err().contains("no generated files under " + actual), output.err());
    }

    @Test
    void allowlistedHandEditIsSkippedAndReported() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        write(expected, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(0.0f, 0.0f);"),
                method("fixture_b", "fixtureB", "assertEquals(2.0f, 2.0f);")));
        write(actual, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);"),
                method("fixture_b", "fixtureB", "assertEquals(2.0f, 2.0f);")));
        List<FreshnessAllowlist.Entry> allowlist = List.of(entry("fixture_a"));

        Output output = compare(expected, actual, allowlist);

        assertTrue(output.result(), output.err());
        assertTrue(output.out().contains(
                "allowlisted hand edit skipped: flex/FlexTest.java :: fixture_a (BK-13 test reason)"),
                output.out());
        assertTrue(output.out().contains("CHECK PASSED: 1 file(s) byte-identical"),
                output.out());
        assertTrue(output.out().contains("(1 allowlisted hand edit(s) skipped)"), output.out());
    }

    @Test
    void nonAllowlistedDifferenceStillFails() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        write(expected, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(0.0f, 0.0f);"),
                method("fixture_b", "fixtureB", "assertEquals(2.0f, 2.0f);")));
        write(actual, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);"),
                method("fixture_b", "fixtureB", "assertEquals(9.0f, 9.0f);")));

        Output output = compare(expected, actual, List.of(entry("fixture_a")));

        assertFalse(output.result());
        assertTrue(output.out().contains("allowlisted hand edit skipped: flex/FlexTest.java :: fixture_a"),
                output.out());
        assertTrue(output.err().contains("CHECK FAILED: differs (after allowlisted methods): "
                + expected.resolve(Path.of("flex", "FlexTest.java"))), output.err());
        assertTrue(output.err().contains("assertEquals(2.0f, 2.0f);"), output.err());
        assertTrue(output.err().contains("assertEquals(9.0f, 9.0f);"), output.err());
    }

    @Test
    void staleAllowlistEntryFails() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        String content = generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);"));
        write(expected, "flex/FlexTest.java", content);
        write(actual, "flex/FlexTest.java", content);

        Output output = compare(expected, actual, List.of(entry("fixture_a")));

        assertFalse(output.result(), "an allowlist entry that suppresses nothing must fail");
        assertTrue(output.err().contains(
                "CHECK FAILED: stale allowlist entry (regeneration already matches): flex/FlexTest.java :: fixture_a"),
                output.err());
        assertTrue(output.err().contains("removeBy=remove after regeneration"), output.err());
    }

    @Test
    void allowlistEntryMissingFromOneSideFails() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        write(expected, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(0.0f, 0.0f);"),
                method("fixture_b", "fixtureB", "assertEquals(2.0f, 2.0f);")));
        write(actual, "flex/FlexTest.java", generatedFile(
                method("fixture_b", "fixtureB", "assertEquals(2.0f, 2.0f);")));

        Output output = compare(expected, actual, List.of(entry("fixture_a")));

        assertFalse(output.result());
        assertTrue(output.err().contains(
                "CHECK FAILED: allowlist entry matches no test method on one side: flex/FlexTest.java :: fixture_a"),
                output.err());
    }

    @Test
    void allowlistEntryOutsideCheckedCategoriesIsReportedButPasses() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        write(expected, "flex/FlexTest.java", generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);")));
        write(expected, "leaf/LeafTest.java", generatedFile(
                method("fixture_leaf", "fixtureLeaf", "assertEquals(1.0f, 1.0f);")));
        write(actual, "leaf/LeafTest.java", generatedFile(
                method("fixture_leaf", "fixtureLeaf", "assertEquals(1.0f, 1.0f);")));

        Output output = compare(expected, actual, List.of(entry("fixture_a")));

        assertTrue(output.result(), output.err());
        assertTrue(output.out().contains(
                "[info] freshness allowlist entry not evaluated (file outside this check): "
                        + "flex/FlexTest.java :: fixture_a"), output.out());
    }

    @Test
    void allowlistEntryForUnknownFileFails() throws Exception {
        Path expected = tempDir.resolve("expected");
        Path actual = tempDir.resolve("actual");
        String content = generatedFile(
                method("fixture_a", "fixtureA", "assertEquals(1.0f, 1.0f);"));
        write(expected, "flex/FlexTest.java", content);
        write(actual, "flex/FlexTest.java", content);

        Output output = compare(expected, actual, List.of(new FreshnessAllowlist.Entry(
                Path.of("grid", "GridTest.java"), "fixture_a", "reason", "since", "removeBy")));

        assertFalse(output.result());
        assertTrue(output.err().contains(
                "CHECK FAILED: allowlist entry matches no generated method: grid/GridTest.java :: fixture_a"),
                output.err());
    }

    private Output compare(Path expected, Path actual, List<FreshnessAllowlist.Entry> allowlist)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        boolean result;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
            result = Generator.compareTrees(expected, actual, REFRESH_COMMAND, allowlist);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
        return new Output(result, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }

    private static FreshnessAllowlist.Entry entry(String displayName) {
        return new FreshnessAllowlist.Entry(Path.of("flex", "FlexTest.java"), displayName,
                "BK-13 test reason", "2026-11-19 a9490f8", "remove after regeneration");
    }

    private static String generatedFile(String... methods) {
        StringBuilder content = new StringBuilder();
        content.append("package dev.flex;\n");
        content.append("\n");
        content.append("public class FlexTest {\n");
        content.append("\n");
        for (String method : methods) {
            content.append(method);
        }
        content.append("}\n");
        return content.toString();
    }

    private static String method(String displayName, String methodName, String... bodyLines) {
        StringBuilder content = new StringBuilder();
        content.append("    @Test\n");
        content.append("    @DisplayName(\"").append(displayName).append("\")\n");
        content.append("    void ").append(methodName).append("() {\n");
        for (String line : bodyLines) {
            content.append("        ").append(line).append('\n');
        }
        content.append("    }\n");
        content.append("\n");
        return content.toString();
    }

    private static void write(Path root, String relative, String content) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private record Output(boolean result, String out, String err) {}
}
