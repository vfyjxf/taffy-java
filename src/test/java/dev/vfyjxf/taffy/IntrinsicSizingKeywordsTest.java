package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CSS intrinsic sizing keywords support (Yoga-like API).
 * These keywords include: min-content, max-content, fit-content, stretch
 */
@DisplayName("Intrinsic Sizing Keywords Tests")
public class IntrinsicSizingKeywordsTest {

    @Nested
    @DisplayName("Dimension Tests")
    class DimensionTests {
        
        @Test
        @DisplayName("minContent() creates MIN_CONTENT type")
        void testMinContent() {
            TaffyDimension dim = TaffyDimension.minContent();
            
            assertEquals(TaffyDimension.Type.MIN_CONTENT, dim.getType());
            assertTrue(dim.isMinContent());
            assertFalse(dim.isLength());
            assertFalse(dim.isPercent());
            assertFalse(dim.isAuto());
            assertTrue(dim.isIntrinsic());
        }
        
        @Test
        @DisplayName("maxContent() creates MAX_CONTENT type")
        void testMaxContent() {
            TaffyDimension dim = TaffyDimension.maxContent();
            
            assertEquals(TaffyDimension.Type.MAX_CONTENT, dim.getType());
            assertTrue(dim.isMaxContent());
            assertFalse(dim.isLength());
            assertFalse(dim.isPercent());
            assertFalse(dim.isAuto());
            assertTrue(dim.isIntrinsic());
        }
        
        @Test
        @DisplayName("fitContent() creates FIT_CONTENT type")
        void testFitContent() {
            TaffyDimension dim = TaffyDimension.fitContent();
            
            assertEquals(TaffyDimension.Type.FIT_CONTENT, dim.getType());
            assertTrue(dim.isFitContent());
            assertFalse(dim.isLength());
            assertFalse(dim.isPercent());
            assertFalse(dim.isAuto());
            assertTrue(dim.isIntrinsic());
        }
        
        @Test
        @DisplayName("stretch() creates STRETCH type")
        void testStretch() {
            TaffyDimension dim = TaffyDimension.stretch();
            
            assertEquals(TaffyDimension.Type.STRETCH, dim.getType());
            assertTrue(dim.isStretch());
            assertFalse(dim.isLength());
            assertFalse(dim.isPercent());
            assertFalse(dim.isAuto());
            assertTrue(dim.isIntrinsic());
        }
        
        @Test
        @DisplayName("Intrinsic keywords are singletons")
        void testSingletons() {
            assertSame(TaffyDimension.minContent(), TaffyDimension.MIN_CONTENT);
            assertSame(TaffyDimension.maxContent(), TaffyDimension.MAX_CONTENT);
            assertSame(TaffyDimension.fitContent(), TaffyDimension.FIT_CONTENT);
            assertSame(TaffyDimension.stretch(), TaffyDimension.STRETCH);
        }
        
        @Test
        @DisplayName("Intrinsic keywords resolve to NaN")
        void testResolveToNaN() {
            assertTrue(Float.isNaN(TaffyDimension.minContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.maxContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.fitContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(TaffyDimension.stretch().maybeResolve(100f)));
        }
        
        @Test
        @DisplayName("Intrinsic keywords resolveOrZero returns zero")
        void testResolveOrZero() {
            assertEquals(0f, TaffyDimension.minContent().resolveOrZero(100f));
            assertEquals(0f, TaffyDimension.maxContent().resolveOrZero(100f));
            assertEquals(0f, TaffyDimension.fitContent().resolveOrZero(100f));
            assertEquals(0f, TaffyDimension.stretch().resolveOrZero(100f));
        }
        
        @Test
        @DisplayName("toString() returns correct CSS keyword names")
        void testToString() {
            assertEquals("min-content", TaffyDimension.minContent().toString());
            assertEquals("max-content", TaffyDimension.maxContent().toString());
            assertEquals("fit-content", TaffyDimension.fitContent().toString());
            assertEquals("stretch", TaffyDimension.stretch().toString());
        }
        
        @Test
        @DisplayName("equals() works correctly for intrinsic keywords")
        void testEquals() {
            assertEquals(TaffyDimension.minContent(), TaffyDimension.minContent());
            assertEquals(TaffyDimension.maxContent(), TaffyDimension.maxContent());
            assertEquals(TaffyDimension.fitContent(), TaffyDimension.fitContent());
            assertEquals(TaffyDimension.stretch(), TaffyDimension.stretch());
            
            assertNotEquals(TaffyDimension.minContent(), TaffyDimension.maxContent());
            assertNotEquals(TaffyDimension.fitContent(), TaffyDimension.stretch());
            assertNotEquals(TaffyDimension.minContent(), TaffyDimension.auto());
            assertNotEquals(TaffyDimension.stretch(), TaffyDimension.length(100));
        }
        
        @Test
        @DisplayName("hashCode() is consistent")
        void testHashCode() {
            assertEquals(TaffyDimension.minContent().hashCode(), TaffyDimension.minContent().hashCode());
            assertEquals(TaffyDimension.maxContent().hashCode(), TaffyDimension.maxContent().hashCode());
            assertEquals(TaffyDimension.fitContent().hashCode(), TaffyDimension.fitContent().hashCode());
            assertEquals(TaffyDimension.stretch().hashCode(), TaffyDimension.stretch().hashCode());
        }
        
        @Test
        @DisplayName("Non-intrinsic types return false for isIntrinsic()")
        void testIsIntrinsicFalseForOthers() {
            assertFalse(TaffyDimension.length(100).isIntrinsic());
            assertFalse(TaffyDimension.percent(0.5f).isIntrinsic());
            assertFalse(TaffyDimension.auto().isIntrinsic());
        }
        
        @Test
        @DisplayName("from(LengthPercentageAuto) converts intrinsic keywords correctly")
        void testFromLengthPercentageAuto() {
            assertEquals(TaffyDimension.minContent(), TaffyDimension.from(LengthPercentageAuto.minContent()));
            assertEquals(TaffyDimension.maxContent(), TaffyDimension.from(LengthPercentageAuto.maxContent()));
            assertEquals(TaffyDimension.fitContent(), TaffyDimension.from(LengthPercentageAuto.fitContent()));
            assertEquals(TaffyDimension.stretch(), TaffyDimension.from(LengthPercentageAuto.stretch()));
        }
    }
    
    @Nested
    @DisplayName("LengthPercentageAuto Tests")
    class LengthPercentageAutoTests {
        
        @Test
        @DisplayName("minContent() creates MIN_CONTENT type")
        void testMinContent() {
            LengthPercentageAuto lpa = LengthPercentageAuto.minContent();
            
            assertEquals(LengthPercentageAuto.Type.MIN_CONTENT, lpa.getType());
            assertTrue(lpa.isMinContent());
            assertFalse(lpa.isLength());
            assertFalse(lpa.isPercent());
            assertFalse(lpa.isAuto());
            assertTrue(lpa.isIntrinsic());
        }
        
        @Test
        @DisplayName("maxContent() creates MAX_CONTENT type")
        void testMaxContent() {
            LengthPercentageAuto lpa = LengthPercentageAuto.maxContent();
            
            assertEquals(LengthPercentageAuto.Type.MAX_CONTENT, lpa.getType());
            assertTrue(lpa.isMaxContent());
            assertFalse(lpa.isLength());
            assertFalse(lpa.isPercent());
            assertFalse(lpa.isAuto());
            assertTrue(lpa.isIntrinsic());
        }
        
        @Test
        @DisplayName("fitContent() creates FIT_CONTENT type")
        void testFitContent() {
            LengthPercentageAuto lpa = LengthPercentageAuto.fitContent();
            
            assertEquals(LengthPercentageAuto.Type.FIT_CONTENT, lpa.getType());
            assertTrue(lpa.isFitContent());
            assertFalse(lpa.isLength());
            assertFalse(lpa.isPercent());
            assertFalse(lpa.isAuto());
            assertTrue(lpa.isIntrinsic());
        }
        
        @Test
        @DisplayName("stretch() creates STRETCH type")
        void testStretch() {
            LengthPercentageAuto lpa = LengthPercentageAuto.stretch();
            
            assertEquals(LengthPercentageAuto.Type.STRETCH, lpa.getType());
            assertTrue(lpa.isStretch());
            assertFalse(lpa.isLength());
            assertFalse(lpa.isPercent());
            assertFalse(lpa.isAuto());
            assertTrue(lpa.isIntrinsic());
        }
        
        @Test
        @DisplayName("Intrinsic keywords are singletons")
        void testSingletons() {
            assertSame(LengthPercentageAuto.minContent(), LengthPercentageAuto.MIN_CONTENT);
            assertSame(LengthPercentageAuto.maxContent(), LengthPercentageAuto.MAX_CONTENT);
            assertSame(LengthPercentageAuto.fitContent(), LengthPercentageAuto.FIT_CONTENT);
            assertSame(LengthPercentageAuto.stretch(), LengthPercentageAuto.STRETCH);
        }
        
        @Test
        @DisplayName("Intrinsic keywords resolve to NaN")
        void testResolveToNaN() {
            assertTrue(Float.isNaN(LengthPercentageAuto.minContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(LengthPercentageAuto.maxContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(LengthPercentageAuto.fitContent().maybeResolve(100f)));
            assertTrue(Float.isNaN(LengthPercentageAuto.stretch().maybeResolve(100f)));
            
            assertTrue(Float.isNaN(LengthPercentageAuto.minContent().resolveToOption(100f)));
            assertTrue(Float.isNaN(LengthPercentageAuto.maxContent().resolveToOption(100f)));
            assertTrue(Float.isNaN(LengthPercentageAuto.fitContent().resolveToOption(100f)));
            assertTrue(Float.isNaN(LengthPercentageAuto.stretch().resolveToOption(100f)));
        }
        
        @Test
        @DisplayName("toString() returns correct CSS keyword names")
        void testToString() {
            assertEquals("min-content", LengthPercentageAuto.minContent().toString());
            assertEquals("max-content", LengthPercentageAuto.maxContent().toString());
            assertEquals("fit-content", LengthPercentageAuto.fitContent().toString());
            assertEquals("stretch", LengthPercentageAuto.stretch().toString());
        }
        
        @Test
        @DisplayName("equals() works correctly for intrinsic keywords")
        void testEquals() {
            assertEquals(LengthPercentageAuto.minContent(), LengthPercentageAuto.minContent());
            assertEquals(LengthPercentageAuto.maxContent(), LengthPercentageAuto.maxContent());
            assertEquals(LengthPercentageAuto.fitContent(), LengthPercentageAuto.fitContent());
            assertEquals(LengthPercentageAuto.stretch(), LengthPercentageAuto.stretch());
            
            assertNotEquals(LengthPercentageAuto.minContent(), LengthPercentageAuto.maxContent());
            assertNotEquals(LengthPercentageAuto.fitContent(), LengthPercentageAuto.stretch());
            assertNotEquals(LengthPercentageAuto.minContent(), LengthPercentageAuto.auto());
        }
    }
    
    @Nested
    @DisplayName("Type Enumeration Tests")
    class TypeEnumTests {
        
        @Test
        @DisplayName("Dimension.Type contains all 8 types")
        void testDimensionTypeCount() {
            assertEquals(8, TaffyDimension.Type.values().length);
        }
        
        @Test
        @DisplayName("LengthPercentageAuto.Type contains all 8 types")
        void testLengthPercentageAutoTypeCount() {
            assertEquals(8, LengthPercentageAuto.Type.values().length);
        }
        
        @Test
        @DisplayName("Dimension types are in expected order")
        void testDimensionTypeOrder() {
            TaffyDimension.Type[] types = TaffyDimension.Type.values();
            assertEquals(TaffyDimension.Type.LENGTH, types[0]);
            assertEquals(TaffyDimension.Type.PERCENT, types[1]);
            assertEquals(TaffyDimension.Type.AUTO, types[2]);
            assertEquals(TaffyDimension.Type.CALC, types[3]);
            assertEquals(TaffyDimension.Type.MIN_CONTENT, types[4]);
            assertEquals(TaffyDimension.Type.MAX_CONTENT, types[5]);
            assertEquals(TaffyDimension.Type.FIT_CONTENT, types[6]);
            assertEquals(TaffyDimension.Type.STRETCH, types[7]);
        }
    }
    
    @Nested
    @DisplayName("Yoga API Compatibility Tests")
    class YogaCompatibilityTests {
        
        @Test
        @DisplayName("All Yoga unit types are supported")
        void testYogaUnitTypesParity() {
            // Yoga supports: UNDEFINED, POINT, PERCENT, AUTO, MAX_CONTENT, FIT_CONTENT, STRETCH
            // We map UNDEFINED -> using NaN values or null
            // We have: LENGTH (POINT), PERCENT, AUTO, CALC, MIN_CONTENT, MAX_CONTENT, FIT_CONTENT, STRETCH
            
            assertNotNull(TaffyDimension.length(100));   // POINT
            assertNotNull(TaffyDimension.percent(0.5f)); // PERCENT
            assertNotNull(TaffyDimension.auto());        // AUTO
            assertNotNull(TaffyDimension.minContent());  // MIN_CONTENT (we added this, Yoga doesn't have it for dimensions)
            assertNotNull(TaffyDimension.maxContent());  // MAX_CONTENT
            assertNotNull(TaffyDimension.fitContent());  // FIT_CONTENT
            assertNotNull(TaffyDimension.stretch());     // STRETCH
        }
        
        @Test
        @DisplayName("Can create Yoga-style width/height dimensions")
        void testYogaStyleDimensions() {
            // Yoga setWidthAuto, setWidthMaxContent, setWidthFitContent, setWidthStretch equivalents
            TaffyDimension widthAuto = TaffyDimension.auto();
            TaffyDimension widthMaxContent = TaffyDimension.maxContent();
            TaffyDimension widthFitContent = TaffyDimension.fitContent();
            TaffyDimension widthStretch = TaffyDimension.stretch();
            
            assertTrue(widthAuto.isAuto());
            assertTrue(widthMaxContent.isMaxContent());
            assertTrue(widthFitContent.isFitContent());
            assertTrue(widthStretch.isStretch());
        }
        
        @Test
        @DisplayName("Can create Yoga-style min/max dimensions")
        void testYogaStyleMinMaxDimensions() {
            // Yoga setMinWidthMaxContent, setMinWidthFitContent, setMinWidthStretch equivalents
            TaffyDimension minWidthMaxContent = TaffyDimension.maxContent();
            TaffyDimension minWidthFitContent = TaffyDimension.fitContent();
            TaffyDimension minWidthStretch = TaffyDimension.stretch();
            
            assertTrue(minWidthMaxContent.isMaxContent());
            assertTrue(minWidthFitContent.isFitContent());
            assertTrue(minWidthStretch.isStretch());
        }
        
        @Test
        @DisplayName("Can create Yoga-style flex-basis dimensions")
        void testYogaStyleFlexBasis() {
            // Yoga setFlexBasisAuto, setFlexBasisMaxContent, setFlexBasisFitContent, setFlexBasisStretch equivalents
            TaffyDimension flexBasisAuto = TaffyDimension.auto();
            TaffyDimension flexBasisMaxContent = TaffyDimension.maxContent();
            TaffyDimension flexBasisFitContent = TaffyDimension.fitContent();
            TaffyDimension flexBasisStretch = TaffyDimension.stretch();
            
            assertTrue(flexBasisAuto.isAuto());
            assertTrue(flexBasisMaxContent.isMaxContent());
            assertTrue(flexBasisFitContent.isFitContent());
            assertTrue(flexBasisStretch.isStretch());
        }
    }
}
