package dev.vfyjxf.taffy.gentest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for the load/style readiness contract (#1054). */
class PageReadinessTest {

    private static final String PAGE = "file:///fixtures/flex/flex_basis_simple.html";

    @Test
    void acceptsTheAppliedMarker() {
        assertTrue(PageReadiness.isBaseStylesheetApplied(PageReadiness.APPLIED_RESULT));
    }

    @Test
    void rejectsMissingStylesheetWithDiagnostics() {
        String probe = "missing: boxSizing=border-box, borderTopColor=rgb(0, 0, 0), "
                + "rtlDirection=ltr, ltrDirection=ltr";
        assertFalse(PageReadiness.isBaseStylesheetApplied(probe));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> {
                    throw new IllegalStateException(
                            PageReadiness.stylesheetFailureMessage(probe, PAGE, "interactive"));
                });
        assertTrue(ex.getMessage().contains(PAGE), ex.getMessage());
        assertTrue(ex.getMessage().contains("document.readyState=interactive"), ex.getMessage());
        assertTrue(ex.getMessage().contains("base stylesheet"), ex.getMessage());
        assertTrue(ex.getMessage().contains("border-box"), ex.getMessage());
    }

    @Test
    void loadWaitUsesTheLoadEvent() {
        assertTrue(PageReadiness.JS_AWAIT_LOAD.contains("document.readyState === 'complete'"));
        assertTrue(PageReadiness.JS_AWAIT_LOAD.contains("addEventListener('load'"));
        assertTrue(PageReadiness.JS_AWAIT_LOAD.contains("arguments[arguments.length - 1]"));
    }

    @Test
    void stylesheetProbeChecksBothBodyClassVariants() {
        assertTrue(PageReadiness.JS_BASE_STYLESHEET_APPLIED.contains("'content-box rtl'"));
        assertTrue(PageReadiness.JS_BASE_STYLESHEET_APPLIED.contains("'content-box ltr'"));
        assertTrue(PageReadiness.JS_BASE_STYLESHEET_APPLIED.contains("rtlBoxSizing === 'content-box'"));
        assertTrue(PageReadiness.JS_BASE_STYLESHEET_APPLIED.contains("borderTopColor === 'rgb(255, 0, 0)'"));
        // The probe must restore the page's own body classes, otherwise
        // getTestData() would measure the wrong variant.
        assertTrue(PageReadiness.JS_BASE_STYLESHEET_APPLIED.contains("body.className = previous"));
    }

    @Test
    void stylesheetProbeToleratesPreDirectionFixtureTrees() {
        // Historical fixtures have no .ltr/.rtl rules; the probe must accept
        // "no direction variants" while still requiring the red border proof.
        assertTrue(PageReadiness.JS_BASE_STYLESHEET_APPLIED.contains("directionVariantsAbsent"));
        assertTrue(PageReadiness.JS_BASE_STYLESHEET_APPLIED.contains("borderTopColor === 'red'"));
    }

    @Test
    void timeoutMessageNamesPageAndReadyState() {
        String message = PageReadiness.loadTimeoutMessage(PAGE, "loading");
        assertTrue(message.contains(PAGE), message);
        assertTrue(message.contains("loading"), message);
    }
}
