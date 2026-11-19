package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.Clear;
import dev.vfyjxf.taffy.style.TaffyFloat;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P7/FT-01 regression tests for the CSS {@code float}/{@code clear} style foundation.
 *
 * <p><b>Scope.</b> These tests cover only the additive types of the first FT-01 batch:
 * {@link TaffyFloat}, {@link Clear} and the two {@link TaffyStyle} fields. No layout algorithm
 * reads them yet, so the assertions here are deliberately style-level (defaults and
 * {@link TaffyStyle#copy()} propagation) rather than layout-level.</p>
 *
 * <p><b>Naming.</b> The upstream Rust type is {@code Float}; the Java port uses
 * {@link TaffyFloat} because a top-level {@code dev.vfyjxf.taffy.style.Float} would shadow
 * {@link java.lang.Float} inside the style package (existing {@code Float.NaN}/{@code isNaN}
 * uses) and make {@code import dev.vfyjxf.taffy.style.*} ambiguous in generated tests. The
 * {@code Taffy}-prefix convention matches {@code TaffyDisplay}/{@code TaffyPosition}.</p>
 */
public class FloatStyleP7Test {

    // === TaffyFloat ===

    @Test
    @DisplayName("TaffyFloat exposes NONE/LEFT/RIGHT and the CSS default NONE")
    void floatEnumValuesAndDefault() {
        assertEquals(3, TaffyFloat.values().length);
        assertSame(TaffyFloat.NONE, TaffyFloat.valueOf("NONE"));
        assertSame(TaffyFloat.LEFT, TaffyFloat.valueOf("LEFT"));
        assertSame(TaffyFloat.RIGHT, TaffyFloat.valueOf("RIGHT"));

        TaffyStyle style = new TaffyStyle();
        assertSame(TaffyFloat.NONE, style.float_);
        assertSame(TaffyFloat.NONE, style.getFloat());
    }

    @Test
    @DisplayName("TaffyFloat.isFloated/isLeft/isRight follow the CSS keywords")
    void floatEnumQueries() {
        assertFalse(TaffyFloat.NONE.isFloated());
        assertFalse(TaffyFloat.NONE.isLeft());
        assertFalse(TaffyFloat.NONE.isRight());

        assertTrue(TaffyFloat.LEFT.isFloated());
        assertTrue(TaffyFloat.LEFT.isLeft());
        assertFalse(TaffyFloat.LEFT.isRight());

        assertTrue(TaffyFloat.RIGHT.isFloated());
        assertFalse(TaffyFloat.RIGHT.isLeft());
        assertTrue(TaffyFloat.RIGHT.isRight());
    }

    // === Clear ===

    @Test
    @DisplayName("Clear exposes NONE/LEFT/RIGHT/BOTH and the CSS default NONE")
    void clearEnumValuesAndDefault() {
        assertEquals(4, Clear.values().length);
        assertSame(Clear.NONE, Clear.valueOf("NONE"));
        assertSame(Clear.LEFT, Clear.valueOf("LEFT"));
        assertSame(Clear.RIGHT, Clear.valueOf("RIGHT"));
        assertSame(Clear.BOTH, Clear.valueOf("BOTH"));

        TaffyStyle style = new TaffyStyle();
        assertSame(Clear.NONE, style.clear);
        assertSame(Clear.NONE, style.getClear());
    }

    @Test
    @DisplayName("Clear.clearsLeft/clearsRight cover the BOTH shorthand")
    void clearEnumQueries() {
        assertFalse(Clear.NONE.clearsLeft());
        assertFalse(Clear.NONE.clearsRight());

        assertTrue(Clear.LEFT.clearsLeft());
        assertFalse(Clear.LEFT.clearsRight());

        assertFalse(Clear.RIGHT.clearsLeft());
        assertTrue(Clear.RIGHT.clearsRight());

        assertTrue(Clear.BOTH.clearsLeft());
        assertTrue(Clear.BOTH.clearsRight());
    }

    // === TaffyStyle fields and copy() ===

    @Test
    @DisplayName("TaffyStyle.copy() preserves float_ and clear")
    void copyPreservesFloatAndClear() {
        TaffyStyle style = new TaffyStyle();
        style.float_ = TaffyFloat.RIGHT;
        style.clear = Clear.BOTH;

        TaffyStyle copy = style.copy();

        assertSame(TaffyFloat.RIGHT, copy.float_);
        assertSame(TaffyFloat.RIGHT, copy.getFloat());
        assertSame(Clear.BOTH, copy.clear);
        assertSame(Clear.BOTH, copy.getClear());
    }

    @Test
    @DisplayName("TaffyStyle.copy() keeps the CSS defaults when the fields are untouched")
    void copyPreservesDefaults() {
        TaffyStyle copy = new TaffyStyle().copy();

        assertSame(TaffyFloat.NONE, copy.float_);
        assertSame(Clear.NONE, copy.clear);
    }
}
