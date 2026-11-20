package dev.vfyjxf.taffy.gentest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parsed command line options for the unified test generator.
 *
 * <p>The CLI intentionally accepts both the new kebab-case options and the old
 * camelCase spellings that were used by {@code TestGenerator} and
 * {@code WptTestGenerator}. Old spellings print a one-line deprecation hint so
 * existing scripts keep working while being migrated.</p>
 */
final class GeneratorOptions {

    enum Source {
        TAFFY,
        WPT,
        ALL
    }

    static final String DEFAULT_TAFFY_PACKAGE = "dev.vfyjxf.taffy.generated";
    static final String DEFAULT_WPT_PACKAGE = "dev.vfyjxf.taffy.wpt";
    static final String DEFAULT_WPT_SUITES = "css/css-grid,css/css-flexbox,css/css-sizing/aspect-ratio";

    final Source source;
    final Path taffyRoot;
    final Path wptRoot;
    final Path wptCssRoot;
    final Path outRoot;
    final String packageName;
    final Set<String> categories = new LinkedHashSet<>();
    final List<String> filters = new ArrayList<>();
    final Path filterFile;
    final Path freshnessAllowlist;
    final Path chromeBinary;
    final Path chromeDriver;
    final boolean check;
    final boolean dryRun;
    final boolean help;

    final boolean convertToFixtures;
    final Path convertInput;
    final Path convertOutput;
    final String convertPrefix;
    final String convertPattern;
    final boolean convertRecursive;

    final List<String> wptSuites = new ArrayList<>();
    final int maxFiles;
    final Path acceptedOut;

    private GeneratorOptions(Builder b) {
        this.source = b.source;
        this.taffyRoot = b.taffyRoot;
        this.wptRoot = b.wptRoot;
        this.wptCssRoot = b.wptCssRoot;
        this.outRoot = b.outRoot;
        this.packageName = b.packageName;
        this.categories.addAll(b.categories);
        this.filters.addAll(b.filters);
        this.filterFile = b.filterFile;
        this.freshnessAllowlist = b.freshnessAllowlist;
        this.chromeBinary = b.chromeBinary;
        this.chromeDriver = b.chromeDriver;
        this.check = b.check;
        this.dryRun = b.dryRun;
        this.help = b.help;
        this.convertToFixtures = b.convertToFixtures;
        this.convertInput = b.convertInput;
        this.convertOutput = b.convertOutput;
        this.convertPrefix = b.convertPrefix;
        this.convertPattern = b.convertPattern;
        this.convertRecursive = b.convertRecursive;
        this.wptSuites.addAll(b.wptSuites);
        this.maxFiles = b.maxFiles;
        this.acceptedOut = b.acceptedOut;
        if (this.wptSuites.isEmpty()) {
            for (String suite : DEFAULT_WPT_SUITES.split(",")) {
                this.wptSuites.add(suite.trim());
            }
        }
    }

    String taffyPackage() {
        return packageName != null ? packageName : DEFAULT_TAFFY_PACKAGE;
    }

    String wptPackage() {
        return packageName != null ? packageName : DEFAULT_WPT_PACKAGE;
    }

    boolean wantsTaffy() {
        return source == Source.TAFFY || source == Source.ALL;
    }

    boolean wantsWpt() {
        return source == Source.WPT || source == Source.ALL;
    }

    static Builder builder() {
        return new Builder();
    }

    static final class Builder {
        Source source = Source.TAFFY;
        Path taffyRoot;
        Path wptRoot;
        Path wptCssRoot;
        Path outRoot;
        String packageName;
        final Set<String> categories = new LinkedHashSet<>();
        final List<String> filters = new ArrayList<>();
        Path filterFile;
        Path freshnessAllowlist;
        Path chromeBinary;
        Path chromeDriver;
        boolean check;
        boolean dryRun;
        boolean help;
        boolean convertToFixtures;
        Path convertInput;
        Path convertOutput;
        String convertPrefix = "wpt_";
        String convertPattern = "*.html";
        boolean convertRecursive;
        final List<String> wptSuites = new ArrayList<>();
        int maxFiles;
        Path acceptedOut;

        GeneratorOptions build() {
            return new GeneratorOptions(this);
        }
    }

    static GeneratorOptions parse(String[] args) {
        Builder b = builder();
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        boolean sawLegacy = false;

        for (int i = 0; i < args.length; i++) {
            String raw = args[i];
            String arg = raw.trim();
            switch (arg) {
                case "--help", "-h" -> b.help = true;
                case "--source" -> b.source = parseSource(value(args, ++i, arg));
                case "--taffy-root", "--taffyRoot" -> {
                    sawLegacy |= arg.equals("--taffyRoot");
                    b.taffyRoot = resolve(cwd, value(args, ++i, arg));
                }
                case "--wpt-root", "--wptRoot" -> {
                    sawLegacy |= arg.equals("--wptRoot");
                    b.wptRoot = resolve(cwd, value(args, ++i, arg));
                }
                case "--wpt-css-root", "--cssRoot" -> {
                    sawLegacy |= arg.equals("--cssRoot");
                    b.wptCssRoot = resolve(cwd, value(args, ++i, arg));
                }
                case "--out", "--out-root", "--outRoot" -> {
                    sawLegacy |= arg.equals("--outRoot");
                    b.outRoot = resolve(cwd, value(args, ++i, arg));
                }
                case "--package" -> b.packageName = value(args, ++i, arg);
                case "--category" -> addCsv(b.categories, value(args, ++i, arg));
                case "--filter" -> addCsv(b.filters, value(args, ++i, arg));
                case "--filter-file", "--allowlist" -> {
                    sawLegacy |= arg.equals("--allowlist");
                    b.filterFile = resolve(cwd, value(args, ++i, arg));
                }
                case "--freshness-allowlist" ->
                        b.freshnessAllowlist = resolve(cwd, value(args, ++i, arg));
                case "--chrome-binary", "--chromeBinary" -> {
                    sawLegacy |= arg.equals("--chromeBinary");
                    b.chromeBinary = resolve(cwd, value(args, ++i, arg));
                }
                case "--chrome-driver", "--chromeDriver" -> {
                    sawLegacy |= arg.equals("--chromeDriver");
                    b.chromeDriver = resolve(cwd, value(args, ++i, arg));
                }
                case "--check" -> b.check = true;
                case "--dry-run" -> b.dryRun = true;
                case "--convert-to-fixtures" -> b.convertToFixtures = true;
                case "--convert-input" -> b.convertInput = resolve(cwd, value(args, ++i, arg));
                case "--convert-output" -> b.convertOutput = resolve(cwd, value(args, ++i, arg));
                case "--prefix" -> b.convertPrefix = value(args, ++i, arg);
                case "--pattern" -> b.convertPattern = value(args, ++i, arg);
                case "--recursive" -> b.convertRecursive = true;
                case "--suites", "--wpt-suites" -> {
                    sawLegacy |= arg.equals("--suites");
                    for (String suite : value(args, ++i, arg).split(",")) {
                        if (!suite.isBlank()) {
                            b.wptSuites.add(suite.trim());
                        }
                    }
                }
                case "--max-files", "--maxFiles" -> {
                    sawLegacy |= arg.equals("--maxFiles");
                    b.maxFiles = parseInt(value(args, ++i, arg), arg);
                }
                case "--accepted-out", "--acceptedOut" -> {
                    sawLegacy |= arg.equals("--acceptedOut");
                    b.acceptedOut = resolve(cwd, value(args, ++i, arg));
                }
                case "--discover" -> {
                    // Old WptTestGenerator flag: discovery is now always explicit via --wpt-suites.
                    sawLegacy = true;
                }
                default -> throw new UsageException("Unknown option: " + arg + "\nRun with --help for usage.");
            }
        }

        if (b.help) {
            return b.build();
        }
        if (sawLegacy) {
            System.err.println("[deprecated] camelCase/legacy options are accepted for migration; " +
                    "see gentest/README.md for the kebab-case equivalents.");
        }

        if (b.taffyRoot == null) {
            // Historical workspace layout: <workspace>/taffy/test_fixtures.
            Path legacy = cwd.resolve("taffy");
            if (Files.isDirectory(legacy.resolve("test_fixtures"))) {
                System.err.println("[info] using legacy workspace layout: " + legacy);
                b.taffyRoot = legacy;
            }
        }
        if (b.taffyRoot == null) {
            Path candidate = cwd.resolve("../taffy").normalize();
            if (Files.isDirectory(candidate)) {
                b.taffyRoot = candidate;
            }
        }
        if (b.taffyRoot == null) {
            String env = env("TAFFY_ROOT");
            if (env != null) {
                b.taffyRoot = resolve(cwd, env);
            }
        }
        if (b.wptRoot == null) {
            String env = env("WPT_ROOT");
            if (env != null) {
                b.wptRoot = resolve(cwd, env);
            }
        }
        if (b.wptRoot == null) {
            Path candidate = cwd.resolve("../wpt").normalize();
            if (Files.isDirectory(candidate)) {
                b.wptRoot = candidate;
            }
        }
        if (b.chromeBinary == null) {
            String env = env("TAFFY_CHROME_BINARY");
            if (env == null || env.isBlank()) {
                env = env("CHROME_BIN");
            }
            if (env == null || env.isBlank()) {
                env = env("CHROME_PATH");
            }
            if (env != null && !env.isBlank()) {
                b.chromeBinary = resolve(cwd, env);
            }
        }
        if (b.outRoot == null) {
            Path legacy = cwd.resolve("taffy-java/src/test/java");
            if (Files.isDirectory(cwd.resolve("taffy")) && Files.isDirectory(legacy)) {
                b.outRoot = legacy.normalize();
            } else {
                b.outRoot = cwd.resolve("src/test/java").normalize();
            }
        }

        if (b.source == null) {
            b.source = Source.TAFFY;
        }
        if (b.convertToFixtures) {
            if (b.source != Source.WPT) {
                throw new UsageException("--convert-to-fixtures requires --source wpt");
            }
            if (b.convertInput == null) {
                throw new UsageException("--convert-to-fixtures requires --convert-input <dir>");
            }
            if (b.convertOutput == null) {
                throw new UsageException("--convert-to-fixtures requires --convert-output <dir>");
            }
        }
        if (b.check && b.dryRun) {
            throw new UsageException("--check and --dry-run are mutually exclusive");
        }
        if (b.freshnessAllowlist != null && !b.check) {
            throw new UsageException("--freshness-allowlist requires --check");
        }
        if (b.freshnessAllowlist != null && b.source == Source.WPT) {
            throw new UsageException("--freshness-allowlist is taffy-only; use --source taffy or --source all");
        }
        return b.build();
    }

    private static Source parseSource(String value) {
        try {
            return Source.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new UsageException("--source must be one of: taffy, wpt, all (got '" + value + "')");
        }
    }

    private static void addCsv(Set<String> target, String csv) {
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                target.add(trimmed);
            }
        }
    }

    private static void addCsv(List<String> target, String csv) {
        for (String part : csv.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                target.add(trimmed);
            }
        }
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new UsageException(option + " requires a value");
        }
        return args[index];
    }

    private static int parseInt(String value, String option) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new UsageException(option + " expects an integer (got '" + value + "')");
        }
    }

    private static Path resolve(Path cwd, String raw) {
        if (raw == null || raw.isBlank()) {
            throw new UsageException("Empty path value");
        }
        Path path = Paths.get(raw);
        if (!path.isAbsolute()) {
            path = cwd.resolve(path);
        }
        path = path.normalize();
        Path repaired = tryRepairWindowsPath(path);
        return repaired != null ? repaired : path;
    }

    /**
     * Historical workaround for Windows paths whose backslashes were eaten by
     * Gradle property escaping (e.g. {@code D:\\foo\\barcss}). Only applies on
     * Windows and only when the repaired directory actually exists.
     */
    private static Path tryRepairWindowsPath(Path path) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win") || Files.isDirectory(path)) {
            return null;
        }
        String raw = path.toString();
        for (String token : new String[]{"css", "resources"}) {
            int index = raw.toLowerCase(Locale.ROOT).lastIndexOf(token);
            if (index <= 0) {
                continue;
            }
            char previous = raw.charAt(index - 1);
            if (previous == '\\' || previous == '/') {
                continue;
            }
            Path candidate = Paths.get(raw.substring(0, index) + java.io.File.separator + raw.substring(index));
            if (Files.isDirectory(candidate)) {
                System.err.println("[warn] repaired Windows path: " + raw + " -> " + candidate);
                return candidate.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static String env(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
    }

    static String usage() {
        return """
                taffy-java test generator (unified taffy fixture + WPT pipeline)

                Usage:
                  Generator --source taffy|wpt|all [options]

                Sources:
                  --source <kind>            taffy (default), wpt, or all
                  --taffy-root <dir>         Rust taffy checkout containing test_fixtures/ and scripts/
                                             (default: ../taffy when it exists, then TAFFY_ROOT)
                  --wpt-root <dir>           WPT checkout root (default: ../wpt when it exists, then WPT_ROOT)
                  --wpt-css-root <dir>       Optional WPT css/ root for split checkouts (default: <wpt-root>)
                  --wpt-suites <csv>         WPT suites to discover (default: %s)

                Output:
                  --out <dir>                Java source root (default: src/test/java)
                  --package <name>           Base Java package (defaults: %s for taffy, %s for WPT)
                  --category <csv>           Only generate these taffy categories / WPT suites
                  --filter <glob[,glob]>     Only fixtures whose relative path matches a glob
                  --filter-file <file>       Only fixtures listed (one root-relative path per line)
                  --accepted-out <file>      WPT: write the generated relative paths to this file
                  --max-files <n>            WPT: cap the number of discovered tests (0 = no cap)

                Browser:
                  --chrome-binary <path>     Chrome binary (overrides environment/auto detection)
                  --chrome-driver <path>     chromedriver binary (overrides Selenium Manager)
                     Environment: TAFFY_CHROME_BINARY, CHROME_BIN, CHROME_PATH
                     Auto order: option > environment > common platform paths > Selenium Manager

                Verification:
                  --check                    Generate to a temp dir and byte-compare with --out
                  --freshness-allowlist <f>  --check: accepted hand edits, one per line:
                                             <path> | <@DisplayName> | reason=... | since=... | removeBy=...
                  --dry-run                  Discover and print planned work without launching Chrome

                WPT fixture conversion:
                  --source wpt --convert-to-fixtures --convert-input <dir> --convert-output <dir>
                  [--prefix wpt_] [--pattern '*.html'] [--recursive]

                Recommendations / migration:
                  --chromeBinary/--chromeDriver/--category/--outRoot/--wptRoot/--cssRoot/--allowlist/
                  --discover/--suites/--maxFiles/--acceptedOut are accepted as deprecated aliases.

                Exit codes: 0 success, 1 generation/check failure, 2 usage error.
                """.formatted(DEFAULT_WPT_SUITES, DEFAULT_TAFFY_PACKAGE, DEFAULT_WPT_PACKAGE);
    }

    static final class UsageException extends RuntimeException {
        UsageException(String message) {
            super(message);
        }
    }

    static void requireDirectory(Path path, String label, String hint) {
        if (path == null) {
            throw new UsageException("Missing " + label + ". " + hint);
        }
        if (!Files.isDirectory(path)) {
            throw new UsageException(label + " is not a directory: " + path + "\n" + hint);
        }
    }

    static Path requireFile(Path path, String label) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            throw new UsageException(label + " is not a file: " + path);
        }
        return path;
    }
}
