package dev.vfyjxf.taffy.gentest;

/**
 * Legacy entry point kept so existing scripts and the Gradle application
 * configuration keep working.
 *
 * @deprecated use {@link Generator}. The unified CLI accepts the old
 *         {@code --chromeBinary}, {@code --chromeDriver} and {@code --category}
 *         spellings with migration warnings.
 */
@Deprecated
public final class TestGenerator {

    private TestGenerator() {}

    public static void main(String[] args) {
        Generator.main(args);
    }
}
