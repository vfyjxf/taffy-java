package dev.vfyjxf.taffy.gentest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for the 15px scrollbar contract (#1016). */
class ScrollbarWidthTest {

    private static final String PAGE = "file:///fixtures/leaf/leaf_overflow_scrollbars.html";

    @AfterEach
    void clearOverride() {
        System.clearProperty(ScrollbarWidth.EXPECTED_WIDTH_PROPERTY);
    }

    @Test
    void defaultsToFifteenPixels() {
        assertEquals(15f, ScrollbarWidth.expectedPx());
    }

    @Test
    void acceptsTheForcedWidth() {
        ScrollbarWidth.verify(15f, PAGE);
        ScrollbarWidth.verify(15.0f, PAGE);
    }

    @Test
    void rejectsPlatformDrift() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ScrollbarWidth.verify(12f, PAGE));
        assertTrue(ex.getMessage().contains("12.0px"), ex.getMessage());
        assertTrue(ex.getMessage().contains("15.0px"), ex.getMessage());
        assertTrue(ex.getMessage().contains(PAGE), ex.getMessage());
        assertTrue(ex.getMessage().contains(ScrollbarWidth.EXPECTED_WIDTH_PROPERTY), ex.getMessage());
    }

    @Test
    void rejectsZeroWidth() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ScrollbarWidth.verify(0f, PAGE));
        assertTrue(ex.getMessage().contains("unexpected scrollbar width"), ex.getMessage());
    }

    @Test
    void overrideAllowsAnExplicitPlatformWidth() {
        System.setProperty(ScrollbarWidth.EXPECTED_WIDTH_PROPERTY, "12");
        ScrollbarWidth.verify(12f, PAGE);
        assertThrows(IllegalStateException.class, () -> ScrollbarWidth.verify(15f, PAGE));
    }

    @Test
    void rejectsInvalidOverride() {
        System.setProperty(ScrollbarWidth.EXPECTED_WIDTH_PROPERTY, "not-a-number");
        IllegalStateException ex = assertThrows(IllegalStateException.class, ScrollbarWidth::expectedPx);
        assertTrue(ex.getMessage().contains(ScrollbarWidth.EXPECTED_WIDTH_PROPERTY), ex.getMessage());
        assertTrue(ex.getMessage().contains("not-a-number"), ex.getMessage());
    }

    @Test
    void rejectsNonPositiveOverride() {
        System.setProperty(ScrollbarWidth.EXPECTED_WIDTH_PROPERTY, "0");
        assertThrows(IllegalStateException.class, ScrollbarWidth::expectedPx);

        System.setProperty(ScrollbarWidth.EXPECTED_WIDTH_PROPERTY, "-1");
        assertThrows(IllegalStateException.class, ScrollbarWidth::expectedPx);
    }
}
