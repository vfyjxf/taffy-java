package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CSS direction property (LTR/RTL) support.
 */
@DisplayName("Direction (RTL/LTR)")
class DirectionTest {

    private static final float EPSILON = 0.1f;

    @Nested
    @DisplayName("Direction Enum")
    class DirectionEnumTests {

        @Test
        @DisplayName("default direction is LTR")
        void defaultDirectionIsLtr() {
            assertEquals(TaffyDirection.LTR, TaffyDirection.DEFAULT);
        }

        @Test
        @DisplayName("isLtr returns correct values")
        void isLtrReturnsCorrectValues() {
            assertTrue(TaffyDirection.LTR.isLtr());
            assertFalse(TaffyDirection.RTL.isLtr());
        }

        @Test
        @DisplayName("isRtl returns correct values")
        void isRtlReturnsCorrectValues() {
            assertFalse(TaffyDirection.LTR.isRtl());
            assertTrue(TaffyDirection.RTL.isRtl());
        }

        @Test
        @DisplayName("opposite returns correct direction")
        void oppositeReturnsCorrectDirection() {
            assertEquals(TaffyDirection.RTL, TaffyDirection.LTR.opposite());
            assertEquals(TaffyDirection.LTR, TaffyDirection.RTL.opposite());
        }
    }

    @Nested
    @DisplayName("Style Direction")
    class StyleDirectionTests {

        @Test
        @DisplayName("style default direction is INHERIT (CSS inheritance)")
        void styleDefaultDirectionIsInherit() {
            TaffyStyle style = new TaffyStyle();
            // Per CSS spec, direction is an inherited property
            // Default should be INHERIT to enable CSS-like inheritance behavior
            assertEquals(TaffyDirection.INHERIT, style.direction);
        }

        @Test
        @DisplayName("style direction can be set to RTL")
        void styleDirectionCanBeSetToRtl() {
            TaffyStyle style = new TaffyStyle();
            style.direction = TaffyDirection.RTL;
            assertEquals(TaffyDirection.RTL, style.direction);
        }

        @Test
        @DisplayName("style direction can be set to LTR")
        void styleDirectionCanBeSetToLtr() {
            TaffyStyle style = new TaffyStyle();
            style.direction = TaffyDirection.LTR;
            assertEquals(TaffyDirection.LTR, style.direction);
        }

        @Test
        @DisplayName("style copy preserves direction")
        void styleCopyPreservesDirection() {
            TaffyStyle style = new TaffyStyle();
            style.direction = TaffyDirection.RTL;
            TaffyStyle copy = style.copy();
            assertEquals(TaffyDirection.RTL, copy.direction);
        }

        @Test
        @DisplayName("getDirection returns correct value")
        void getDirectionReturnsCorrectValue() {
            TaffyStyle style = new TaffyStyle();
            style.direction = TaffyDirection.RTL;
            assertEquals(TaffyDirection.RTL, style.getDirection());
        }
    }

    @Nested
    @DisplayName("Flexbox RTL Layout")
    class FlexboxRtlLayoutTests {

        @Test
        @DisplayName("row direction with LTR lays out items left to right")
        void rowDirectionWithLtrLayoutsLeftToRight() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle containerStyle = new TaffyStyle();
            containerStyle.display = TaffyDisplay.FLEX;
            containerStyle.flexDirection = FlexDirection.ROW;
            containerStyle.direction = TaffyDirection.LTR;
            containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));

            TaffyStyle childStyle1 = new TaffyStyle();
            childStyle1.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            TaffyStyle childStyle2 = new TaffyStyle();
            childStyle2.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            NodeId child1 = tree.newLeaf(childStyle1);
            NodeId child2 = tree.newLeaf(childStyle2);
            NodeId container = tree.newWithChildren(containerStyle, child1, child2);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout child1Layout = tree.getLayout(child1);
            Layout child2Layout = tree.getLayout(child2);

            // LTR: child1 at x=0, child2 at x=100
            assertEquals(0f, child1Layout.location().x, EPSILON, "child1 x should be 0 (LTR)");
            assertEquals(100f, child2Layout.location().x, EPSILON, "child2 x should be 100 (LTR)");
        }

        @Test
        @DisplayName("row direction with RTL lays out items right to left")
        void rowDirectionWithRtlLayoutsRightToLeft() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle containerStyle = new TaffyStyle();
            containerStyle.display = TaffyDisplay.FLEX;
            containerStyle.flexDirection = FlexDirection.ROW;
            containerStyle.direction = TaffyDirection.RTL;
            containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));

            TaffyStyle childStyle1 = new TaffyStyle();
            childStyle1.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            TaffyStyle childStyle2 = new TaffyStyle();
            childStyle2.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            NodeId child1 = tree.newLeaf(childStyle1);
            NodeId child2 = tree.newLeaf(childStyle2);
            NodeId container = tree.newWithChildren(containerStyle, child1, child2);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout child1Layout = tree.getLayout(child1);
            Layout child2Layout = tree.getLayout(child2);

            // RTL: items flow from right to left
            // child1 (first in DOM) is at the right side: x = 300 - 100 = 200
            // child2 (second in DOM) is to the left of child1: x = 200 - 100 = 100
            assertEquals(200f, child1Layout.location().x, EPSILON, "child1 x should be 200 (RTL, rightmost)");
            assertEquals(100f, child2Layout.location().x, EPSILON, "child2 x should be 100 (RTL, left of child1)");
        }

        @Test
        @DisplayName("row-reverse with RTL lays out items left to right (double reverse)")
        void rowReverseWithRtlLayoutsLeftToRight() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle containerStyle = new TaffyStyle();
            containerStyle.display = TaffyDisplay.FLEX;
            containerStyle.flexDirection = FlexDirection.ROW_REVERSE;
            containerStyle.direction = TaffyDirection.RTL;
            containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));

            TaffyStyle childStyle1 = new TaffyStyle();
            childStyle1.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            TaffyStyle childStyle2 = new TaffyStyle();
            childStyle2.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            NodeId child1 = tree.newLeaf(childStyle1);
            NodeId child2 = tree.newLeaf(childStyle2);
            NodeId container = tree.newWithChildren(containerStyle, child1, child2);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout child1Layout = tree.getLayout(child1);
            Layout child2Layout = tree.getLayout(child2);

            // RTL + row-reverse = left to right (double negative)
            // First child should be at x=0
            assertEquals(0f, child1Layout.location().x, EPSILON, "child1 x should be 0 (RTL + row-reverse)");
            assertEquals(100f, child2Layout.location().x, EPSILON, "child2 x should be 100 (RTL + row-reverse)");
        }

        @Test
        @DisplayName("column direction not affected by RTL")
        void columnDirectionNotAffectedByRtl() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle containerStyle = new TaffyStyle();
            containerStyle.display = TaffyDisplay.FLEX;
            containerStyle.flexDirection = FlexDirection.COLUMN;
            containerStyle.direction = TaffyDirection.RTL;
            containerStyle.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(300f));

            TaffyStyle childStyle1 = new TaffyStyle();
            childStyle1.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(100f));

            TaffyStyle childStyle2 = new TaffyStyle();
            childStyle2.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(100f));

            NodeId child1 = tree.newLeaf(childStyle1);
            NodeId child2 = tree.newLeaf(childStyle2);
            NodeId container = tree.newWithChildren(containerStyle, child1, child2);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout child1Layout = tree.getLayout(child1);
            Layout child2Layout = tree.getLayout(child2);

            // Column direction is not affected by RTL - items flow top to bottom
            assertEquals(0f, child1Layout.location().y, EPSILON, "child1 y should be 0 (column top)");
            assertEquals(100f, child2Layout.location().y, EPSILON, "child2 y should be 100 (column after child1)");
        }

        @Test
        @DisplayName("three children with RTL direction")
        void threeChildrenWithRtlDirection() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle containerStyle = new TaffyStyle();
            containerStyle.display = TaffyDisplay.FLEX;
            containerStyle.flexDirection = FlexDirection.ROW;
            containerStyle.direction = TaffyDirection.RTL;
            containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));

            TaffyStyle childStyle1 = new TaffyStyle();
            childStyle1.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));

            TaffyStyle childStyle2 = new TaffyStyle();
            childStyle2.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            TaffyStyle childStyle3 = new TaffyStyle();
            childStyle3.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));

            NodeId child1 = tree.newLeaf(childStyle1);
            NodeId child2 = tree.newLeaf(childStyle2);
            NodeId child3 = tree.newLeaf(childStyle3);
            NodeId container = tree.newWithChildren(containerStyle, child1, child2, child3);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout child1Layout = tree.getLayout(child1);
            Layout child2Layout = tree.getLayout(child2);
            Layout child3Layout = tree.getLayout(child3);

            // Total width = 50 + 100 + 50 = 200
            // Container = 300, so free space = 100
            // RTL with flex-start: items start from right edge
            // child1 (50px): x = 300 - 50 = 250
            // child2 (100px): x = 250 - 100 = 150
            // child3 (50px): x = 150 - 50 = 100
            assertEquals(250f, child1Layout.location().x, EPSILON, "child1 should be at rightmost (RTL)");
            assertEquals(150f, child2Layout.location().x, EPSILON, "child2 should be in middle (RTL)");
            assertEquals(100f, child3Layout.location().x, EPSILON, "child3 should be at left of others (RTL)");
        }
    }

    @Nested
    @DisplayName("Direction Inheritance (CSS spec)")
    class DirectionInheritanceTests {

        @Test
        @DisplayName("root with INHERIT resolves to LTR (CSS initial value)")
        void rootWithInheritResolvesToLtr() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle containerStyle = new TaffyStyle();
            containerStyle.display = TaffyDisplay.FLEX;
            containerStyle.flexDirection = FlexDirection.ROW;
            // direction defaults to INHERIT
            containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));

            TaffyStyle childStyle1 = new TaffyStyle();
            childStyle1.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            TaffyStyle childStyle2 = new TaffyStyle();
            childStyle2.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            NodeId child1 = tree.newLeaf(childStyle1);
            NodeId child2 = tree.newLeaf(childStyle2);
            NodeId container = tree.newWithChildren(containerStyle, child1, child2);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout child1Layout = tree.getLayout(child1);
            Layout child2Layout = tree.getLayout(child2);

            // Root with INHERIT should use CSS initial value LTR
            // LTR: child1 at x=0, child2 at x=100
            assertEquals(0f, child1Layout.location().x, EPSILON, "child1 x should be 0 (inherited LTR)");
            assertEquals(100f, child2Layout.location().x, EPSILON, "child2 x should be 100 (inherited LTR)");
        }

        @Test
        @DisplayName("child inherits RTL from parent")
        void childInheritsRtlFromParent() {
            TaffyTree tree = new TaffyTree();

            TaffyStyle containerStyle = new TaffyStyle();
            containerStyle.display = TaffyDisplay.FLEX;
            containerStyle.flexDirection = FlexDirection.ROW;
            containerStyle.direction = TaffyDirection.RTL;  // Explicit RTL
            containerStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(100f));

            TaffyStyle childStyle1 = new TaffyStyle();
            // direction defaults to INHERIT - should inherit RTL from parent
            childStyle1.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            TaffyStyle childStyle2 = new TaffyStyle();
            // direction defaults to INHERIT - should inherit RTL from parent
            childStyle2.size = new TaffySize<>(TaffyDimension.length(100f), TaffyDimension.length(50f));

            NodeId child1 = tree.newLeaf(childStyle1);
            NodeId child2 = tree.newLeaf(childStyle2);
            NodeId container = tree.newWithChildren(containerStyle, child1, child2);

            tree.computeLayout(container, TaffySize.maxContent());

            Layout child1Layout = tree.getLayout(child1);
            Layout child2Layout = tree.getLayout(child2);

            // Children should inherit RTL from parent
            // RTL: child1 at x=200, child2 at x=100
            assertEquals(200f, child1Layout.location().x, EPSILON, "child1 x should be 200 (inherited RTL)");
            assertEquals(100f, child2Layout.location().x, EPSILON, "child2 x should be 100 (inherited RTL)");
        }

        @Test
        @DisplayName("nested INHERIT propagates through hierarchy")
        void nestedInheritPropagatesThroughHierarchy() {
            TaffyTree tree = new TaffyTree();

            // Root: explicit RTL
            TaffyStyle rootStyle = new TaffyStyle();
            rootStyle.display = TaffyDisplay.FLEX;
            rootStyle.flexDirection = FlexDirection.ROW;
            rootStyle.direction = TaffyDirection.RTL;
            rootStyle.size = new TaffySize<>(TaffyDimension.length(400f), TaffyDimension.length(200f));

            // Middle container: INHERIT (should get RTL from root)
            TaffyStyle middleStyle = new TaffyStyle();
            middleStyle.display = TaffyDisplay.FLEX;
            middleStyle.flexDirection = FlexDirection.ROW;
            // direction defaults to INHERIT
            middleStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));

            // Leaf children: INHERIT (should get RTL from middle, which got it from root)
            TaffyStyle leafStyle1 = new TaffyStyle();
            leafStyle1.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));

            TaffyStyle leafStyle2 = new TaffyStyle();
            leafStyle2.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));

            NodeId leaf1 = tree.newLeaf(leafStyle1);
            NodeId leaf2 = tree.newLeaf(leafStyle2);
            NodeId middle = tree.newWithChildren(middleStyle, leaf1, leaf2);
            NodeId root = tree.newWithChildren(rootStyle, middle);

            tree.computeLayout(root, TaffySize.maxContent());

            Layout middleLayout = tree.getLayout(middle);
            Layout leaf1Layout = tree.getLayout(leaf1);
            Layout leaf2Layout = tree.getLayout(leaf2);

            // Middle should be positioned RTL within root (400 - 200 = 200)
            assertEquals(200f, middleLayout.location().x, EPSILON, "middle should be at x=200 (RTL from root)");

            // Leaves within middle: RTL inherited through hierarchy
            // leaf1 at x = 200 - 50 = 150, leaf2 at x = 150 - 50 = 100
            assertEquals(150f, leaf1Layout.location().x, EPSILON, "leaf1 should be at x=150 (inherited RTL)");
            assertEquals(100f, leaf2Layout.location().x, EPSILON, "leaf2 should be at x=100 (inherited RTL)");
        }

        @Test
        @DisplayName("explicit LTR overrides inherited RTL")
        void explicitLtrOverridesInheritedRtl() {
            TaffyTree tree = new TaffyTree();

            // Root: explicit RTL
            TaffyStyle rootStyle = new TaffyStyle();
            rootStyle.display = TaffyDisplay.FLEX;
            rootStyle.flexDirection = FlexDirection.ROW;
            rootStyle.direction = TaffyDirection.RTL;
            rootStyle.size = new TaffySize<>(TaffyDimension.length(300f), TaffyDimension.length(200f));

            // Child container: explicit LTR (overrides parent's RTL)
            TaffyStyle childContainerStyle = new TaffyStyle();
            childContainerStyle.display = TaffyDisplay.FLEX;
            childContainerStyle.flexDirection = FlexDirection.ROW;
            childContainerStyle.direction = TaffyDirection.LTR;  // Override!
            childContainerStyle.size = new TaffySize<>(TaffyDimension.length(200f), TaffyDimension.length(100f));

            // Grandchildren: INHERIT (should get LTR from child, not RTL from root)
            TaffyStyle grandchildStyle1 = new TaffyStyle();
            grandchildStyle1.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));

            TaffyStyle grandchildStyle2 = new TaffyStyle();
            grandchildStyle2.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(50f));

            NodeId grandchild1 = tree.newLeaf(grandchildStyle1);
            NodeId grandchild2 = tree.newLeaf(grandchildStyle2);
            NodeId childContainer = tree.newWithChildren(childContainerStyle, grandchild1, grandchild2);
            NodeId root = tree.newWithChildren(rootStyle, childContainer);

            tree.computeLayout(root, TaffySize.maxContent());

            Layout childContainerLayout = tree.getLayout(childContainer);
            Layout grandchild1Layout = tree.getLayout(grandchild1);
            Layout grandchild2Layout = tree.getLayout(grandchild2);

            // Child container is positioned RTL within root (300 - 200 = 100)
            assertEquals(100f, childContainerLayout.location().x, EPSILON, "childContainer should be at x=100 (RTL in root)");

            // Grandchildren within childContainer: LTR (overridden)
            // grandchild1 at x=0, grandchild2 at x=50
            assertEquals(0f, grandchild1Layout.location().x, EPSILON, "grandchild1 should be at x=0 (LTR override)");
            assertEquals(50f, grandchild2Layout.location().x, EPSILON, "grandchild2 should be at x=50 (LTR override)");
        }
    }
}
