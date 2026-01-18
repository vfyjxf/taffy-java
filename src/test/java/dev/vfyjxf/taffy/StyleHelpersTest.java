package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.GridRepetition;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Style helper tests ported from taffy/src/style_helpers.rs
 */
public class StyleHelpersTest {

    @Test
    @DisplayName("test_repeat_u16")
    void testRepeatU16() {
        GridTemplateComponent comp = GridTemplateComponent.repeatCount(123);
        assertTrue(comp.isRepeat());
        GridRepetition repeat = comp.getRepeat();
        assertNotNull(repeat);
        assertEquals(GridRepetition.RepetitionType.COUNT, repeat.getType());
        assertEquals(123, repeat.getCount());
        assertEquals(0, repeat.getTracks().size());
    }

    @Test
    @DisplayName("test_repeat_auto_fit_str")
    void testRepeatAutoFitStr() {
        GridTemplateComponent comp = GridTemplateComponent.autoFit();
        assertTrue(comp.isRepeat());
        GridRepetition repeat = comp.getRepeat();
        assertNotNull(repeat);
        assertEquals(GridRepetition.RepetitionType.AUTO_FIT, repeat.getType());
        assertEquals(0, repeat.getCount());
        assertEquals(List.of(), repeat.getTracks());
    }

    @Test
    @DisplayName("test_repeat_auto_fill_str")
    void testRepeatAutoFillStr() {
        GridTemplateComponent comp = GridTemplateComponent.autoFill();
        assertTrue(comp.isRepeat());
        GridRepetition repeat = comp.getRepeat();
        assertNotNull(repeat);
        assertEquals(GridRepetition.RepetitionType.AUTO_FILL, repeat.getType());
        assertEquals(0, repeat.getCount());
        assertEquals(List.of(), repeat.getTracks());
    }
}
