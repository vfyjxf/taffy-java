package dev.vfyjxf.taffy.gentest;

/**
 * Contract between the Chrome scrollbar probe and the generated tests.
 *
 * <p>Taffy's {@code test_base_style.css} forces a 15px space-taking scrollbar
 * ({@code ::-webkit-scrollbar { width: 15px; height: 15px; }}), and the
 * generated Java tests encode that width in {@code style.scrollbarWidth}.
 * If the browser reports a different width the generated expectations are no
 * longer the upstream ones, so generation fails loudly instead of silently
 * drifting.</p>
 *
 * <p>The expected width can be overridden for CI platforms that genuinely use
 * a different scrollbar width:
 * {@code -Dtaffy.gentest.expectedScrollbarWidth=<px>}. Overriding is an
 * explicit decision and should be recorded wherever the generated tests are
 * checked in.</p>
 */
final class ScrollbarWidth {

    /** System property that overrides {@link #DEFAULT_EXPECTED_PX}. */
    static final String EXPECTED_WIDTH_PROPERTY = "taffy.gentest.expectedScrollbarWidth";

    /** Width forced by test_base_style.css (#1016). */
    static final float DEFAULT_EXPECTED_PX = 15f;

    private static final float TOLERANCE_PX = 0.01f;

    private ScrollbarWidth() {}

    /**
     * A platform-level contract violation. Unlike per-fixture parse/style
     * errors this is fatal: generating a suite with silently different
     * scrollbar assumptions must not look like success.
     */
    static final class ContractViolation extends IllegalStateException {
        ContractViolation(String message) {
            super(message);
        }

        ContractViolation(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Resolve the expected scrollbar width from the system property or the 15px default. */
    static float expectedPx() {
        String raw = System.getProperty(EXPECTED_WIDTH_PROPERTY);
        if (raw == null || raw.isBlank()) {
            return DEFAULT_EXPECTED_PX;
        }
        try {
            float value = Float.parseFloat(raw.trim());
            if (!Float.isFinite(value) || value <= 0f) {
                throw new NumberFormatException("expected a positive finite number");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new ContractViolation(
                    "invalid -D" + EXPECTED_WIDTH_PROPERTY + "='" + raw + "': " + ex.getMessage(), ex);
        }
    }

    /** Assert that the measured width matches the expected contract. */
    static void verify(float measuredPx, String page) {
        float expectedPx = expectedPx();
        if (!Float.isFinite(measuredPx) || Math.abs(measuredPx - expectedPx) > TOLERANCE_PX) {
            throw new ContractViolation(failureMessage(measuredPx, expectedPx, page));
        }
    }

    static String failureMessage(float measuredPx, float expectedPx, String page) {
        return "unexpected scrollbar width: measured " + measuredPx + "px on " + page
                + ", but the generated tests assume " + expectedPx + "px (-D" + EXPECTED_WIDTH_PROPERTY + "). "
                + "The space-taking 15px scrollbar is forced by taffy's test_base_style.css (#1016); "
                + "a different value means the probe page, the stylesheet, or the platform changed. "
                + "Fix the environment, or explicitly opt in with -D" + EXPECTED_WIDTH_PROPERTY + "=" + measuredPx
                + " and regenerate the expectations.";
    }
}
