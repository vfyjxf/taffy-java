package dev.vfyjxf.taffy.compat;

import dev.vfyjxf.taffy.geometry.FloatPoint;
import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Serializes a computed layout tree to the deterministic JSON format used by the
 * P0-a compatibility golden files, and compares (or updates) the checked-in snapshot.
 *
 * <p>The format is intentionally narrow and stable so that a golden diff is reviewable:
 * <ul>
 *   <li>field order is fixed (the writer appends fields in a hard-coded order);</li>
 *   <li>floats are printed as JSON numbers with exactly four decimal places;</li>
 *   <li>nodes are emitted as a flat depth-first list identified by their tree path,
 *       so inserting a scenario helper cannot silently reorder node identities;</li>
 *   <li>UTF-8 with {@code \n} line endings and a trailing newline.</li>
 * </ul>
 *
 * <p>The snapshots intentionally record <em>current</em> behavior, including known
 * P0-a bugs, and are not "expected correct" values. After a P0-a fix is merged, the
 * golden file is expected to change and must be refreshed with
 * {@code -Dtaffy.compat.updateGolden=true}.
 */
final class GoldenSupport {

    /** System property that switches the golden assertions into rewrite mode. */
    static final String UPDATE_PROPERTY = "taffy.compat.updateGolden";

    /** Baseline snapshots captured at commit 49b7ef0 (project version 1.1.4). */
    private static final Path GOLDEN_DIR = Path.of("compat-golden", "p0a-baseline");

    /** Copy of the last actual output, for easy inspection when an assertion fails. */
    private static final Path ACTUAL_DIR = Path.of("build", "compat-golden-actual");

    private static final int DECIMAL_PLACES = 4;

    private GoldenSupport() {
    }

    /**
     * Serializes every node reachable from {@code root} and asserts it against
     * {@code compat-golden/p0a-baseline/&lt;caseName&gt;.json}, or rewrites that file when
     * {@code -Dtaffy.compat.updateGolden=true} is set.
     */
    static void assertGolden(String caseName, String description,
                             float availableWidth, float availableHeight,
                             TaffyTree tree, NodeId root) {
        String actualJson = serialize(caseName, description, availableWidth, availableHeight, tree, root);
        Path goldenFile = GOLDEN_DIR.resolve(caseName + ".json");

        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            try {
                Files.createDirectories(goldenFile.getParent());
                Files.writeString(goldenFile, actualJson, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to write golden file " + goldenFile, e);
            }
            System.out.println("[compat-golden] wrote " + goldenFile.toAbsolutePath());
            return;
        }

        if (!Files.isRegularFile(goldenFile)) {
            Path actualFile = writeActual(caseName, actualJson);
            fail("Missing golden file: " + goldenFile.toAbsolutePath()
                 + System.lineSeparator()
                 + "Actual snapshot was written to: " + actualFile.toAbsolutePath()
                 + System.lineSeparator()
                 + "Create it with: ./gradlew test --tests 'dev.vfyjxf.taffy.compat.*' -D"
                 + UPDATE_PROPERTY + "=true");
            return;
        }

        String expectedJson;
        try {
            expectedJson = Files.readString(goldenFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read golden file " + goldenFile, e);
        }

        if (!expectedJson.equals(actualJson)) {
            Path actualFile = writeActual(caseName, actualJson);
            fail(mismatchMessage(goldenFile, actualFile, expectedJson, actualJson));
        }
    }

    private static Path writeActual(String caseName, String actualJson) {
        Path actualFile = ACTUAL_DIR.resolve(caseName + ".json");
        try {
            Files.createDirectories(actualFile.getParent());
            Files.writeString(actualFile, actualJson, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write actual snapshot " + actualFile, e);
        }
        return actualFile;
    }

    private static String mismatchMessage(Path goldenFile, Path actualFile,
                                          String expectedJson, String actualJson) {
        String[] expectedLines = expectedJson.split("\n", -1);
        String[] actualLines = actualJson.split("\n", -1);
        int maxLines = Math.max(expectedLines.length, actualLines.length);
        int firstDifference = 0;
        while (firstDifference < maxLines
               && Objects.equals(lineAt(expectedLines, firstDifference),
                                 lineAt(actualLines, firstDifference))) {
            firstDifference++;
        }

        StringBuilder message = new StringBuilder(1024);
        message.append("Golden layout snapshot mismatch: ").append(goldenFile).append('\n');
        message.append("These snapshots freeze the current P0-a baseline. If the change is intentional,")
               .append(" refresh them after merging with:\n");
        message.append("  ./gradlew test --tests 'dev.vfyjxf.taffy.compat.*' -D")
               .append(UPDATE_PROPERTY).append("=true\n");
        message.append("Actual JSON: ").append(actualFile.toAbsolutePath()).append('\n');
        message.append("First difference at line ").append(firstDifference + 1).append(":\n");

        int from = Math.max(0, firstDifference - 3);
        int to = Math.min(maxLines, firstDifference + 4);
        for (int i = from; i < to; i++) {
            message.append("  ").append(String.format("%6d", i + 1)).append(" expected | ")
                   .append(lineAt(expectedLines, i)).append('\n');
            message.append("  ").append("      ").append(" actual   | ")
                   .append(lineAt(actualLines, i)).append('\n');
        }
        return message.toString();
    }

    private static String lineAt(String[] lines, int index) {
        return index < lines.length ? lines[index] : "<end of file>";
    }

    /**
     * Builds the deterministic JSON snapshot for the whole tree in depth-first order.
     */
    private static String serialize(String caseName, String description,
                                    float availableWidth, float availableHeight,
                                    TaffyTree tree, NodeId root) {
        StringBuilder json = new StringBuilder(16_384);
        json.append("{\n");
        json.append("  \"case\": ").append(quote(caseName)).append(",\n");
        json.append("  \"description\": ").append(quote(description)).append(",\n");
        json.append("  \"availableSpace\": {\n");
        json.append("    \"width\": ").append(number(availableWidth)).append(",\n");
        json.append("    \"height\": ").append(number(availableHeight)).append("\n");
        json.append("  },\n");
        json.append("  \"roundingEnabled\": ").append(tree.roundingEnabled()).append(",\n");
        json.append("  \"nodes\": [\n");
        boolean[] firstNode = {true};
        appendNode(json, tree, root, "root", firstNode);
        json.append('\n');
        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private static void appendNode(StringBuilder json, TaffyTree tree, NodeId node,
                                   String path, boolean[] firstNode) {
        if (!firstNode[0]) {
            json.append(",\n");
        }
        firstNode[0] = false;

        Layout layout = tree.getLayout(node);
        if (layout == null) {
            throw new IllegalStateException("No layout for node " + path);
        }

        json.append("    {\n");
        json.append("      \"path\": ").append(quote(path)).append(",\n");
        json.append("      \"order\": ").append(layout.order()).append(",\n");
        appendPoint(json, "location", layout.location());
        appendSize(json, "size", layout.size());
        appendRect(json, "margin", layout.margin());
        appendRect(json, "border", layout.border());
        appendRect(json, "padding", layout.padding());
        appendSize(json, "contentSize", layout.contentSize());
        json.append("      \"scrollWidth\": ").append(number(layout.scrollWidth())).append(",\n");
        json.append("      \"scrollHeight\": ").append(number(layout.scrollHeight())).append("\n");
        json.append("    }");

        List<NodeId> children = tree.getChildren(node);
        for (int i = 0; i < children.size(); i++) {
            appendNode(json, tree, children.get(i), path + "/children[" + i + "]", firstNode);
        }
    }

    private static void appendPoint(StringBuilder json, String field, FloatPoint point) {
        json.append("      \"").append(field).append("\": {\n");
        json.append("        \"x\": ").append(number(point.x)).append(",\n");
        json.append("        \"y\": ").append(number(point.y)).append("\n");
        json.append("      },\n");
    }

    private static void appendSize(StringBuilder json, String field, FloatSize size) {
        json.append("      \"").append(field).append("\": {\n");
        json.append("        \"width\": ").append(number(size.width)).append(",\n");
        json.append("        \"height\": ").append(number(size.height)).append("\n");
        json.append("      },\n");
    }

    private static void appendRect(StringBuilder json, String field, FloatRect rect) {
        json.append("      \"").append(field).append("\": {\n");
        json.append("        \"left\": ").append(number(rect.left)).append(",\n");
        json.append("        \"top\": ").append(number(rect.top)).append(",\n");
        json.append("        \"right\": ").append(number(rect.right)).append(",\n");
        json.append("        \"bottom\": ").append(number(rect.bottom)).append("\n");
        json.append("      },\n");
    }

    /**
     * Formats a layout float as a JSON number with a fixed number of decimal places.
     * Layout values are finite after rounding; a non-finite value is a scenario bug.
     */
    private static String number(float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Cannot serialize non-finite layout value " + value);
        }
        BigDecimal decimal = new BigDecimal(Float.toString(value))
            .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
        if (decimal.signum() == 0) {
            decimal = new BigDecimal("0.0000");
        }
        return decimal.toPlainString();
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 2);
        escaped.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }
}
