package dev.vfyjxf.taffy.gentest;

import java.time.Duration;

/**
 * Browser readiness contract shared by the taffy and WPT generators (#1054).
 *
 * <p>Navigation can return before the document is fully loaded, in which case
 * the fixture helper script and stylesheet may not have been applied yet.
 * Measuring such a page yields border-box/ltr values for every variant, so the
 * generator waits for the load event and probes the base stylesheet before
 * reading anything back.</p>
 */
final class PageReadiness {

    /** Upper bound for navigation/load waits; a fixture normally loads in milliseconds. */
    static final Duration TIMEOUT = Duration.ofSeconds(30);

    /** getTestData() retries after a JavaScript error (upstream #1054 backstop). */
    static final int GET_TEST_DATA_ATTEMPTS = 3;

    static final long RETRY_DELAY_MS = 200L;

    static final String JS_READY_STATE = "return document.readyState";

    /**
     * Blocks until the load event fired. Stylesheets are guaranteed to be
     * applied after the load event, so the generator must not measure earlier.
     */
    static final String JS_AWAIT_LOAD = """
            const done = arguments[arguments.length - 1];
            if (document.readyState === 'complete') {
              done(true);
            } else {
              addEventListener('load', () => done(true), { once: true });
            }
            """;

    /**
     * Probes the border-box/content-box (and, when the fixture tree has them,
     * ltr/rtl) classes that getTestData() depends on.
     *
     * <p>Historical taffy fixtures predate direction support: their stylesheet
     * has no {@code .ltr}/{@code .rtl} rules and the helper only switches
     * border-box/content-box. Requiring an rtl direction unconditionally would
     * reject those fixtures, so the probe accepts either of two contracts:</p>
     *
     * <ul>
     *   <li>direction rules present: rtl under {@code .rtl} and ltr under
     *       {@code .ltr};</li>
     *   <li>direction rules absent: computed direction stays ltr for both.</li>
     * </ul>
     *
     * <p>In both cases the stylesheet must prove it participates in the
     * cascade: the probe becomes content-box under {@code .content-box} and
     * gets the red border from {@code div, span, img { border: 0 solid red }}.
     * A missing stylesheet leaves the default content-box but no red border
     * (and, on modern trees, no rtl direction), so it is rejected.</p>
     */
    static final String JS_BASE_STYLESHEET_APPLIED = """
            const body = document.body;
            const previous = body.className;
            const probe = document.createElement('div');
            body.appendChild(probe);
            try {
              body.className = 'content-box rtl';
              const rtlStyle = getComputedStyle(probe);
              const rtlBoxSizing = rtlStyle.boxSizing;
              const rtlDirection = rtlStyle.direction;
              const borderTopColor = rtlStyle.borderTopColor;

              body.className = 'content-box ltr';
              const ltrDirection = getComputedStyle(probe).direction;

              const stylesheetApplied = rtlBoxSizing === 'content-box'
                && (borderTopColor === 'rgb(255, 0, 0)' || borderTopColor === 'red');
              const directionVariantsPresent = rtlDirection === 'rtl' && ltrDirection === 'ltr';
              const directionVariantsAbsent = rtlDirection === 'ltr' && ltrDirection === 'ltr';
              if (stylesheetApplied && (directionVariantsPresent || directionVariantsAbsent)) {
                return 'applied';
              }
              return 'missing: boxSizing=' + rtlBoxSizing
                + ', borderTopColor=' + borderTopColor
                + ', rtlDirection=' + rtlDirection
                + ', ltrDirection=' + ltrDirection;
            } finally {
              probe.remove();
              body.className = previous;
            }
            """;

    /** Result returned by {@link #JS_BASE_STYLESHEET_APPLIED} when the stylesheet is active. */
    static final String APPLIED_RESULT = "applied";

    private PageReadiness() {}

    static boolean isBaseStylesheetApplied(String probeResult) {
        return APPLIED_RESULT.equals(probeResult);
    }

    static String stylesheetFailureMessage(String probeResult, String page, String readyState) {
        return "the taffy test base stylesheet is not applied on " + page
                + " (document.readyState=" + readyState + "): " + probeResult + ". "
                + "Its box-sizing variants (and ltr/rtl rules when present) are required by getTestData(); "
                + "refusing to measure a page with missing styles.";
    }

    static String loadTimeoutMessage(String page, String readyState) {
        return "timed out waiting for " + page + " to finish loading (document.readyState=" + readyState + ")";
    }
}
