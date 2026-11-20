package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PA-04: {@link TaffyStyle#copy()} must carry over the Yoga-style {@code flex} shorthand field.
 * Without it, a copied style silently loses the effect of {@link TaffyStyle#setFlex(float)} and the
 * copied {@code getEffectiveFlexGrow()/getEffectiveFlexShrink()/getEffectiveFlexBasis()} fall back to
 * the individual flex properties.
 */
@DisplayName("TaffyStyle.copy() preserves the flex shorthand (PA-04)")
class StyleCopyFlexTest {

    @Test
    @DisplayName("copy keeps a positive flex shorthand")
    void copyPreservesPositiveFlexShorthand() {
        TaffyStyle style = new TaffyStyle();
        style.setFlex(2.0f);

        TaffyStyle copy = style.copy();

        assertNotSame(style, copy);
        assertEquals(2.0f, copy.getFlex(), 0.0f, "raw flex field must be copied");
        assertEquals(2.0f, copy.getEffectiveFlexGrow(), 0.0f, "effective flex-grow must survive the copy");
        assertEquals(1.0f, copy.getEffectiveFlexShrink(), 0.0f, "effective flex-shrink must survive the copy");
        assertEquals(TaffyDimension.ZERO, copy.getEffectiveFlexBasis(), "effective flex-basis must stay 0");
    }

    @Test
    @DisplayName("copy keeps a negative flex shorthand")
    void copyPreservesNegativeFlexShorthand() {
        TaffyStyle style = new TaffyStyle();
        style.setFlex(-3.0f);

        TaffyStyle copy = style.copy();

        assertEquals(-3.0f, copy.getFlex(), 0.0f, "raw flex field must be copied");
        assertEquals(0.0f, copy.getEffectiveFlexGrow(), 0.0f, "negative flex means grow 0");
        assertEquals(3.0f, copy.getEffectiveFlexShrink(), 0.0f, "negative flex means shrink = -flex");
        assertEquals(TaffyDimension.ZERO, copy.getEffectiveFlexBasis(), "effective flex-basis must stay 0");
    }

    @Test
    @DisplayName("copy keeps the shorthand authoritative over individual flex properties")
    void copyPreservesFlexShorthandPrecedence() {
        TaffyStyle style = new TaffyStyle();
        style.setFlex(1.5f);
        // Individual values are ignored while the shorthand is set; this must also hold for the copy.
        style.flexGrow = 9.0f;
        style.flexShrink = 9.0f;

        TaffyStyle copy = style.copy();

        assertEquals(1.5f, copy.getFlex(), 0.0f, "raw flex field must be copied");
        assertEquals(1.5f, copy.getEffectiveFlexGrow(), 0.0f, "shorthand wins over flexGrow in the copy");
        assertEquals(1.0f, copy.getEffectiveFlexShrink(), 0.0f, "shorthand wins over flexShrink in the copy");
    }

    @Test
    @DisplayName("copy keeps an unset shorthand unset")
    void copyKeepsUnsetShorthandUnset() {
        TaffyStyle style = new TaffyStyle();
        style.flexGrow = 5.0f;
        style.flexShrink = 2.0f;
        style.flexBasis = TaffyDimension.length(10.0f);

        TaffyStyle copy = style.copy();

        assertTrue(Float.isNaN(copy.getFlex()), "unset shorthand must stay NaN");
        assertEquals(5.0f, copy.getEffectiveFlexGrow(), 0.0f, "individual flex-grow is used when shorthand is unset");
        assertEquals(2.0f, copy.getEffectiveFlexShrink(), 0.0f, "individual flex-shrink is used when shorthand is unset");
        assertEquals(TaffyDimension.length(10.0f), copy.getEffectiveFlexBasis(), "individual flex-basis is used when shorthand is unset");
    }

    @Test
    @DisplayName("copy is independent: clearing the shorthand on the copy leaves the original intact")
    void copyIsIndependentOfOriginalShorthand() {
        TaffyStyle style = new TaffyStyle();
        style.setFlex(2.0f);

        TaffyStyle copy = style.copy();
        copy.clearFlex();

        assertEquals(2.0f, style.getFlex(), 0.0f, "original shorthand must not be affected by the copy");
        assertEquals(2.0f, style.getEffectiveFlexGrow(), 0.0f, "original effective flex-grow must not be affected");
        assertTrue(Float.isNaN(copy.getFlex()), "clearing the shorthand on the copy only affects the copy");
        assertEquals(0.0f, copy.getEffectiveFlexGrow(), 0.0f, "copy falls back to its own individual flex-grow");
    }
}
