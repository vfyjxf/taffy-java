package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CSS calc() expression support.
 */
@DisplayName("CSS calc() Support")
class CalcExpressionTest {

    @Nested
    @DisplayName("CalcExpression Interface")
    class CalcExpressionInterfaceTests {
        
        @Test
        @DisplayName("percentPlusLength: calc(50% + 20px)")
        void percentPlusLength() {
            CalcExpression expr = CalcExpression.percentPlusLength(0.5f, 20f);
            assertEquals(70f, expr.resolve(100f), 0.001f);
            assertEquals(120f, expr.resolve(200f), 0.001f);
        }
        
        @Test
        @DisplayName("percentMinusLength: calc(100% - 20px)")
        void percentMinusLength() {
            CalcExpression expr = CalcExpression.percentMinusLength(1.0f, 20f);
            assertEquals(80f, expr.resolve(100f), 0.001f);
            assertEquals(180f, expr.resolve(200f), 0.001f);
        }
        
        @Test
        @DisplayName("fullMinusLength: calc(100% - 50px)")
        void fullMinusLength() {
            CalcExpression expr = CalcExpression.fullMinusLength(50f);
            assertEquals(50f, expr.resolve(100f), 0.001f);
            assertEquals(150f, expr.resolve(200f), 0.001f);
        }
        
        @Test
        @DisplayName("percentDividedBy: calc(100% / 3)")
        void percentDividedBy() {
            CalcExpression expr = CalcExpression.percentDividedBy(1.0f, 3f);
            assertEquals(33.333f, expr.resolve(100f), 0.01f);
        }
        
        @Test
        @DisplayName("custom lambda expression")
        void customLambda() {
            // calc(50% * 2 + 10px)
            CalcExpression expr = basis -> basis * 0.5f * 2 + 10f;
            assertEquals(110f, expr.resolve(100f), 0.001f);
        }
    }

    @Nested
    @DisplayName("LengthPercentage with calc")
    class LengthPercentageCalcTests {
        
        @Test
        @DisplayName("calc type is identified correctly")
        void calcTypeIdentified() {
            LengthPercentage lp = LengthPercentage.calc(basis -> basis * 0.5f);
            assertTrue(lp.isCalc());
            assertFalse(lp.isLength());
            assertFalse(lp.isPercent());
            assertEquals(LengthPercentage.Type.CALC, lp.getType());
        }
        
        @Test
        @DisplayName("calc resolves correctly")
        void calcResolves() {
            LengthPercentage lp = LengthPercentage.calc(CalcExpression.percentMinusLength(1.0f, 20f));
            assertEquals(80f, lp.resolve(100f), 0.001f);
            assertEquals(180f, lp.resolve(200f), 0.001f);
        }
        
        @Test
        @DisplayName("calc maybeResolve returns NaN for NaN context")
        void calcMaybeResolveWithNaN() {
            LengthPercentage lp = LengthPercentage.calc(basis -> basis * 0.5f);
            assertTrue(Float.isNaN(lp.maybeResolve(Float.NaN)));
        }
        
        @Test
        @DisplayName("calc resolveOrZero returns 0 for NaN context")
        void calcResolveOrZero() {
            LengthPercentage lp = LengthPercentage.calc(basis -> basis * 0.5f);
            assertEquals(0f, lp.resolveOrZero(Float.NaN));
            assertEquals(50f, lp.resolveOrZero(100f));
        }
    }

    @Nested
    @DisplayName("LengthPercentageAuto with calc")
    class LengthPercentageAutoCalcTests {
        
        @Test
        @DisplayName("calc type is identified correctly")
        void calcTypeIdentified() {
            LengthPercentageAuto lpa = LengthPercentageAuto.calc(basis -> basis * 0.5f);
            assertTrue(lpa.isCalc());
            assertFalse(lpa.isLength());
            assertFalse(lpa.isPercent());
            assertFalse(lpa.isAuto());
        }
        
        @Test
        @DisplayName("from LengthPercentage preserves calc")
        void fromLengthPercentagePreservesCalc() {
            CalcExpression expr = CalcExpression.fullMinusLength(10f);
            LengthPercentage lp = LengthPercentage.calc(expr);
            LengthPercentageAuto lpa = LengthPercentageAuto.from(lp);
            
            assertTrue(lpa.isCalc());
            assertEquals(90f, lpa.resolveToOption(100f), 0.001f);
        }
    }

    @Nested
    @DisplayName("Dimension with calc")
    class DimensionCalcTests {
        
        @Test
        @DisplayName("calc type is identified correctly")
        void calcTypeIdentified() {
            TaffyDimension dim = TaffyDimension.calc(basis -> basis * 0.5f);
            assertTrue(dim.isCalc());
            assertFalse(dim.isLength());
            assertFalse(dim.isPercent());
            assertFalse(dim.isAuto());
        }
        
        @Test
        @DisplayName("calc resolves correctly")
        void calcResolves() {
            TaffyDimension dim = TaffyDimension.calc(CalcExpression.percentPlusLength(0.5f, 25f));
            assertEquals(75f, dim.maybeResolve(100f), 0.001f);
            assertEquals(125f, dim.maybeResolve(200f), 0.001f);
        }
        
        @Test
        @DisplayName("from LengthPercentage preserves calc")
        void fromLengthPercentagePreservesCalc() {
            CalcExpression expr = basis -> basis - 30f;
            LengthPercentage lp = LengthPercentage.calc(expr);
            TaffyDimension dim = TaffyDimension.from(lp);
            
            assertTrue(dim.isCalc());
            assertEquals(70f, dim.maybeResolve(100f), 0.001f);
        }
        
        @Test
        @DisplayName("from LengthPercentageAuto preserves calc")
        void fromLengthPercentageAutoPreservesCalc() {
            CalcExpression expr = basis -> basis / 4f;
            LengthPercentageAuto lpa = LengthPercentageAuto.calc(expr);
            TaffyDimension dim = TaffyDimension.from(lpa);
            
            assertTrue(dim.isCalc());
            assertEquals(25f, dim.maybeResolve(100f), 0.001f);
        }
    }

    @Nested
    @DisplayName("Layout Integration")
    class LayoutIntegrationTests {
        
        @Test
        @DisplayName("width: calc(100% - 20px) in block layout")
        void widthCalcInBlockLayout() {
            TaffyTree tree = new TaffyTree();
            
            // Parent with fixed size
            TaffyStyle parentStyle = new TaffyStyle();
            parentStyle.display = TaffyDisplay.BLOCK;
            parentStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));
            
            // Child with calc width
            TaffyStyle childStyle = new TaffyStyle();
            childStyle.display = TaffyDisplay.BLOCK;
            childStyle.size = new TaffySize<>(
                TaffyDimension.calc(CalcExpression.fullMinusLength(20f)),  // calc(100% - 20px)
                TaffyDimension.length(50f)
            );
            
            NodeId child = tree.newLeaf(childStyle);
            NodeId parent = tree.newWithChildren(parentStyle, child);
            
            tree.computeLayout(parent, new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.definite(100f)));
            
            Layout childLayout = tree.getLayout(child);
            assertEquals(180f, childLayout.size().width, 0.1f);  // 200 - 20 = 180
            assertEquals(50f, childLayout.size().height, 0.1f);
        }
        
        @Test
        @DisplayName("width: calc(50% + 10px) in flex layout")
        void widthCalcInFlexLayout() {
            TaffyTree tree = new TaffyTree();
            
            // Parent with fixed size, flex display
            TaffyStyle parentStyle = new TaffyStyle();
            parentStyle.display = TaffyDisplay.FLEX;
            parentStyle.flexDirection = FlexDirection.ROW;
            parentStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));
            
            // Child with calc width
            TaffyStyle childStyle = new TaffyStyle();
            childStyle.size = new TaffySize<>(
                TaffyDimension.calc(CalcExpression.percentPlusLength(0.5f, 10f)),  // calc(50% + 10px)
                TaffyDimension.length(50f)
            );
            
            NodeId child = tree.newLeaf(childStyle);
            NodeId parent = tree.newWithChildren(parentStyle, child);
            
            tree.computeLayout(parent, new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.definite(100f)));
            
            Layout childLayout = tree.getLayout(child);
            assertEquals(110f, childLayout.size().width, 0.1f);  // 200*0.5 + 10 = 110
        }
        
        @Test
        @DisplayName("margin with calc expression")
        void marginWithCalc() {
            TaffyTree tree = new TaffyTree();
            
            // Parent
            TaffyStyle parentStyle = new TaffyStyle();
            parentStyle.display = TaffyDisplay.BLOCK;
            parentStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));
            
            // Child with calc margin
            TaffyStyle childStyle = new TaffyStyle();
            childStyle.display = TaffyDisplay.BLOCK;
            childStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));
            childStyle.margin.left = LengthPercentageAuto.calc(
                CalcExpression.percentPlusLength(0.25f, 10f)  // calc(25% + 10px) = 60px
            );
            
            NodeId child = tree.newLeaf(childStyle);
            NodeId parent = tree.newWithChildren(parentStyle, child);
            
            tree.computeLayout(parent, new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.definite(100f)));
            
            Layout childLayout = tree.getLayout(child);
            assertEquals(60f, childLayout.location().x, 0.1f);  // 200*0.25 + 10 = 60
        }
        
        @Test
        @DisplayName("padding with calc expression")
        void paddingWithCalc() {
            TaffyTree tree = new TaffyTree();
            
            // Parent with calc padding
            TaffyStyle parentStyle = new TaffyStyle();
            parentStyle.display = TaffyDisplay.BLOCK;
            parentStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));
            parentStyle.padding.left = LengthPercentage.calc(
                CalcExpression.percentMinusLength(0.1f, 5f)  // calc(10% - 5px) = 15px
            );
            parentStyle.padding.right = LengthPercentage.calc(
                CalcExpression.percentMinusLength(0.1f, 5f)  // calc(10% - 5px) = 15px
            );
            
            // Child filling parent
            TaffyStyle childStyle = new TaffyStyle();
            childStyle.display = TaffyDisplay.BLOCK;
            childStyle.size = new TaffySize<>(TaffyDimension.percent(1.0f), TaffyDimension.length(50f));
            
            NodeId child = tree.newLeaf(childStyle);
            NodeId parent = tree.newWithChildren(parentStyle, child);
            
            tree.computeLayout(parent, new TaffySize<>(AvailableSpace.definite(200f), AvailableSpace.definite(100f)));
            
            Layout childLayout = tree.getLayout(child);
            // Child width = 200 - 15 - 15 = 170
            assertEquals(170f, childLayout.size().width, 0.1f);
            // Child x = 15 (left padding)
            assertEquals(15f, childLayout.location().x, 0.1f);
        }
        
        @Test
        @DisplayName("grid track with calc minmax")
        void gridTrackWithCalc() {
            TaffyTree tree = new TaffyTree();
            
            // Parent grid
            TaffyStyle parentStyle = new TaffyStyle();
            parentStyle.display = TaffyDisplay.GRID;
            parentStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));
            // First column: calc(100% / 3) ≈ 100px
            parentStyle.gridTemplateColumns.add(
                TrackSizingFunction.minmax(
                    TrackSizingFunction.fixed(LengthPercentage.calc(
                        CalcExpression.percentDividedBy(1.0f, 3f)
                    )),
                    TrackSizingFunction.fixed(LengthPercentage.calc(
                        CalcExpression.percentDividedBy(1.0f, 3f)
                    ))
                )
            );
            // Second column: auto
            parentStyle.gridTemplateColumns.add(TrackSizingFunction.AUTO);
            
            TaffyStyle child1Style = new TaffyStyle();
            TaffyStyle child2Style = new TaffyStyle();
            
            NodeId child1 = tree.newLeaf(child1Style);
            NodeId child2 = tree.newLeaf(child2Style);
            NodeId parent = tree.newWithChildren(parentStyle, child1, child2);
            
            tree.computeLayout(parent, new TaffySize<>(AvailableSpace.definite(300f), AvailableSpace.definite(100f)));
            
            Layout child1Layout = tree.getLayout(child1);
            Layout child2Layout = tree.getLayout(child2);
            
            // First column should be ~100px (300 / 3)
            assertEquals(100f, child1Layout.size().width, 1f);
            // Second column gets remaining space (~200px)
            assertEquals(200f, child2Layout.size().width, 1f);
        }
    }

    @Nested
    @DisplayName("Equality and toString")
    class EqualityAndStringTests {
        
        @Test
        @DisplayName("calc values with same expression are equal")
        void calcValuesEqual() {
            CalcExpression expr = CalcExpression.fullMinusLength(20f);
            TaffyDimension dim1 = TaffyDimension.calc(expr);
            TaffyDimension dim2 = TaffyDimension.calc(expr);
            
            assertEquals(dim1, dim2);
            assertEquals(dim1.hashCode(), dim2.hashCode());
        }
        
        @Test
        @DisplayName("calc toString returns calc(...)")
        void calcToString() {
            TaffyDimension dim = TaffyDimension.calc(basis -> basis);
            assertEquals("calc(...)", dim.toString());
            
            LengthPercentage lp = LengthPercentage.calc(basis -> basis);
            assertEquals("calc(...)", lp.toString());
            
            LengthPercentageAuto lpa = LengthPercentageAuto.calc(basis -> basis);
            assertEquals("calc(...)", lpa.toString());
        }
    }
}
