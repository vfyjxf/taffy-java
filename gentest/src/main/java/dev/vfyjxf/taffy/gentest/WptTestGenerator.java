package dev.vfyjxf.taffy.gentest;

import java.util.ArrayList;
import java.util.List;

/**
 * Legacy entry point for the old WPT generator CLI.
 *
 * @deprecated use {@code Generator --source wpt ...}. The old
 *         {@code --wptRoot/--cssRoot/--allowlist/--discover/--suites/--outRoot}
 *         options are still accepted by the unified parser.
 */
@Deprecated
public final class WptTestGenerator {

    private WptTestGenerator() {}

    public static void main(String[] args) {
        List<String> translated = new ArrayList<>();
        translated.add("--source");
        translated.add("wpt");
        boolean hasSource = false;
        for (String arg : args) {
            if ("--source".equals(arg)) {
                hasSource = true;
                break;
            }
        }
        if (hasSource) {
            translated.clear();
        }
        translated.addAll(List.of(args));
        System.err.println("[deprecated] WptTestGenerator is a compatibility shim; " +
                "prefer: Generator --source wpt ...");
        Generator.main(translated.toArray(String[]::new));
    }
}
