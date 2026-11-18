package dev.vfyjxf.taffy.gentest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Naming, formatting and file output shared by all sources. */
final class JavaTestWriter {

    private JavaTestWriter() {}

    static String taffyHeader(String packageName, String category, String className) {
        return "package " + packageName + "." + StyleCodegen.categoryToPackage(category) + ";\n\n"
                + "import dev.vfyjxf.taffy.geometry.*;\n"
                + "import dev.vfyjxf.taffy.style.*;\n"
                + "import dev.vfyjxf.taffy.tree.*;\n"
                + "import dev.vfyjxf.taffy.util.MeasureFunc;\n"
                + "import org.junit.jupiter.api.Test;\n"
                + "import org.junit.jupiter.api.DisplayName;\n"
                + "import static org.junit.jupiter.api.Assertions.*;\n\n"
                + "/**\n * Generated tests for " + category + " layout fixtures.\n */\n"
                + "public class " + className + " {\n\n"
                + "    private static final float EPSILON = 0.1f;\n\n"
                + indent(AHEM_MEASURE_HELPER, "    ");
    }

    static String wptHeader(String packageName, String className) {
        return "package " + packageName + ";\n\n"
                + "import dev.vfyjxf.taffy.geometry.*;\n"
                + "import dev.vfyjxf.taffy.style.*;\n"
                + "import dev.vfyjxf.taffy.tree.*;\n"
                + "import org.junit.jupiter.api.Test;\n"
                + "import org.junit.jupiter.api.DisplayName;\n"
                + "import static org.junit.jupiter.api.Assertions.*;\n\n"
                + "/**\n"
                + " * Generated from WPT.\n"
                + " *\n"
                + " * Source: https://github.com/web-platform-tests/wpt\n"
                + " *\n"
                + " * NOTE: These tests are generated and live under a dedicated package\n"
                + " * (`dev.vfyjxf.taffy.wpt.*`) and are intentionally\n"
                + " * kept separate from taffy fixture-based tests.\n"
                + " */\n"
                + "public class " + className + " {\n\n"
                + "    private static final float EPSILON = 0.5f;\n\n";
    }

    private static final String AHEM_MEASURE_HELPER = """
                private static MeasureFunc ahemTextMeasure(String text, boolean vertical) {
                    final String trimmed = text == null ? "" : text.trim();
                    return (knownDimensions, availableSpace) -> {
                        if (!Float.isNaN(knownDimensions.width) && !Float.isNaN(knownDimensions.height)) {
                            return new FloatSize(knownDimensions.width, knownDimensions.height);
                        }

                        final char ZWS = '\\u200B';
                        final float H_WIDTH = 10.0f;
                        final float H_HEIGHT = 10.0f;

                        String[] parts = trimmed.isEmpty() ? new String[0] : trimmed.split(String.valueOf(ZWS), -1);
                        if (parts.length == 0) {
                            float w = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : 0.0f;
                            float h = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : 0.0f;
                            return new FloatSize(w, h);
                        }

                        int minLineLength = 0;
                        int maxLineLength = 0;
                        for (String p : parts) {
                            int len = p.length();
                            if (len > minLineLength) minLineLength = len;
                            maxLineLength += len;
                        }

                        float knownInline = vertical ? knownDimensions.height : knownDimensions.width;
                        float knownBlock = vertical ? knownDimensions.width : knownDimensions.height;
                        AvailableSpace availInline = vertical ? availableSpace.height : availableSpace.width;

                        float inlineSize;
                        if (!Float.isNaN(knownInline)) {
                            inlineSize = knownInline;
                        } else if (availInline != null && availInline.isMinContent()) {
                            inlineSize = minLineLength * H_WIDTH;
                        } else if (availInline != null && availInline.isMaxContent()) {
                            inlineSize = maxLineLength * H_WIDTH;
                        } else if (availInline != null && availInline.isDefinite()) {
                            inlineSize = Math.min(availInline.getValue(), maxLineLength * H_WIDTH);
                        } else {
                            inlineSize = maxLineLength * H_WIDTH;
                        }
                        inlineSize = Math.max(inlineSize, minLineLength * H_WIDTH);

                        float blockSize;
                        if (!Float.isNaN(knownBlock)) {
                            blockSize = knownBlock;
                        } else {
                            int inlineLineLength = (int) Math.floor(inlineSize / H_WIDTH);
                            int lineCount = 1;
                            int currentLineLength = 0;
                            for (String p : parts) {
                                int len = p.length();
                                if (currentLineLength + len > inlineLineLength) {
                                    if (currentLineLength > 0) {
                                        lineCount += 1;
                                    }
                                    currentLineLength = len;
                                } else {
                                    currentLineLength += len;
                                }
                            }
                            blockSize = lineCount * H_HEIGHT;
                        }

                        FloatSize computed = vertical
                            ? new FloatSize(blockSize, inlineSize)
                            : new FloatSize(inlineSize, blockSize);

                        float outW = !Float.isNaN(knownDimensions.width) ? knownDimensions.width : computed.width;
                        float outH = !Float.isNaN(knownDimensions.height) ? knownDimensions.height : computed.height;
                        return new FloatSize(outW, outH);
                    };
                }

                """;

    private static String indent(String block, String prefix) {
        StringBuilder out = new StringBuilder(block.length());
        String[] lines = block.split("\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            if (!lines[i].isEmpty()) {
                out.append(prefix).append(lines[i]);
            }
        }
        return out.toString();
    }

    static void write(Path file, String content) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        Files.writeString(file, normalized, StandardCharsets.UTF_8);
    }

    static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> ordered = paths.sorted(Comparator.reverseOrder()).toList();
            for (Path path : ordered) {
                Files.delete(path);
            }
        }
    }

    static String fmt3(float value) {
        if (Float.isNaN(value)) {
            return "Float.NaN";
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }

    static String fmtFloat(float value) {
        return Float.toString(value);
    }

    /** Safe Java identifier for WPT method names (historical behavior). */
    static String wptMethodName(String name) {
        String base = name.replaceAll("[^a-zA-Z0-9_]+", "_");
        if (base.isEmpty()) {
            base = "test";
        }
        if (!Character.isJavaIdentifierStart(base.charAt(0))) {
            base = "t_" + base;
        }
        return base;
    }

    static String wptSuiteClass(String suite) {
        return switch (suite) {
            case "grid" -> "WptGridTest";
            case "flexbox" -> "WptFlexboxTest";
            case "sizing_aspect_ratio" -> "WptSizingAspectRatioTest";
            default -> "WptTest";
        };
    }
}
