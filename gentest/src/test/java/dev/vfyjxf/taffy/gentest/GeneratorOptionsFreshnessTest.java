package dev.vfyjxf.taffy.gentest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Option wiring rules for the taffy-only freshness allowlist. */
class GeneratorOptionsFreshnessTest {

    @Test
    void allowlistRequiresCheck() {
        GeneratorOptions.UsageException error = assertThrows(GeneratorOptions.UsageException.class,
                () -> GeneratorOptions.parse(new String[]{"--freshness-allowlist", "allowlist.txt"}));

        assertTrue(error.getMessage().contains("requires --check"), error.getMessage());
    }

    @Test
    void allowlistIsRejectedForWptSource() {
        GeneratorOptions.UsageException error = assertThrows(GeneratorOptions.UsageException.class,
                () -> GeneratorOptions.parse(new String[]{
                        "--source", "wpt", "--check", "--freshness-allowlist", "allowlist.txt"}));

        assertTrue(error.getMessage().contains("taffy-only"), error.getMessage());
    }

    @Test
    void allowlistIsAcceptedForTaffyAndAllSources() {
        GeneratorOptions taffy = GeneratorOptions.parse(new String[]{
                "--check", "--freshness-allowlist", "allowlist.txt"});
        assertNotNull(taffy.freshnessAllowlist);

        GeneratorOptions all = GeneratorOptions.parse(new String[]{
                "--source", "all", "--check", "--freshness-allowlist", "allowlist.txt"});
        assertNotNull(all.freshnessAllowlist);
    }
}
