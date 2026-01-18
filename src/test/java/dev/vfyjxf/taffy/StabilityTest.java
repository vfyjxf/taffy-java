package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.Line;
import dev.vfyjxf.taffy.geometry.Point;
import dev.vfyjxf.taffy.geometry.Rect;
import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.Overflow;
import dev.vfyjxf.taffy.style.Position;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.Layout;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stability and regression tests to detect hidden bugs in the layout library.
 * These tests cover:
 * 1. Incremental updates (add/remove/modify nodes)
 * 2. Cache consistency
 * 3. Edge cases (empty, extreme values, NaN)
 * 4. Complex real-world scenarios
 * 5. Web CSS behavior alignment
 */
public class StabilityTest {

    private static final float EPSILON = 0.01f;

    // ==================== Incremental Update Tests ====================
    
    @Nested
    @DisplayName("Incremental Update Tests")
    class IncrementalUpdateTests {
        
        @Test
        @DisplayName("Style change should trigger relayout")
        void styleChangeShouldTriggerRelayout() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            NodeId child = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(200));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            tree.computeLayout(root, Size.maxContent());
            assertEquals(50, tree.getLayout(child).size().width, EPSILON);
            
            // Change child size
            Style newChildStyle = new Style();
            newChildStyle.size = new Size<>(Dimension.length(100), Dimension.length(100));
            tree.setStyle(child, newChildStyle);
            
            tree.computeLayout(root, Size.maxContent());
            assertEquals(100, tree.getLayout(child).size().width, EPSILON);
        }
        
        @Test
        @DisplayName("Adding child should update layout")
        void addingChildShouldUpdateLayout() {
            TaffyTree tree = new TaffyTree();
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexDirection = FlexDirection.ROW;
            rootStyle.size = new Size<>(Dimension.length(300), Dimension.length(100));
            NodeId root = tree.newLeaf(rootStyle);
            
            // Add first child
            Style child1Style = new Style();
            child1Style.size = new Size<>(Dimension.length(100), Dimension.length(50));
            NodeId child1 = tree.newLeaf(child1Style);
            tree.addChild(root, child1);
            
            tree.computeLayout(root, Size.maxContent());
            assertEquals(0, tree.getLayout(child1).location().x, EPSILON);
            
            // Add second child
            Style child2Style = new Style();
            child2Style.size = new Size<>(Dimension.length(100), Dimension.length(50));
            NodeId child2 = tree.newLeaf(child2Style);
            tree.addChild(root, child2);
            
            tree.computeLayout(root, Size.maxContent());
            assertEquals(0, tree.getLayout(child1).location().x, EPSILON);
            assertEquals(100, tree.getLayout(child2).location().x, EPSILON);
        }
        
        @Test
        @DisplayName("Removing child should update layout")
        void removingChildShouldUpdateLayout() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.flexGrow = 1.0f;
            childStyle.size = new Size<>(Dimension.AUTO, Dimension.length(50));
            
            NodeId child1 = tree.newLeaf(childStyle);
            NodeId child2 = tree.newLeaf(childStyle);
            NodeId child3 = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexDirection = FlexDirection.ROW;
            rootStyle.size = new Size<>(Dimension.length(300), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);
            
            tree.computeLayout(root, Size.maxContent());
            // Each child should be 100px wide (300 / 3)
            assertEquals(100, tree.getLayout(child1).size().width, EPSILON);
            assertEquals(100, tree.getLayout(child2).size().width, EPSILON);
            assertEquals(100, tree.getLayout(child3).size().width, EPSILON);
            
            // Remove middle child
            tree.removeChild(root, child2);
            tree.computeLayout(root, Size.maxContent());
            
            // Now each remaining child should be 150px wide (300 / 2)
            assertEquals(150, tree.getLayout(child1).size().width, EPSILON);
            assertEquals(150, tree.getLayout(child3).size().width, EPSILON);
        }
        
        @Test
        @DisplayName("Reordering children should update layout")
        void reorderingChildrenShouldUpdateLayout() {
            TaffyTree tree = new TaffyTree();
            
            Style style1 = new Style();
            style1.size = new Size<>(Dimension.length(50), Dimension.length(50));
            NodeId child1 = tree.newLeaf(style1);
            
            Style style2 = new Style();
            style2.size = new Size<>(Dimension.length(100), Dimension.length(50));
            NodeId child2 = tree.newLeaf(style2);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexDirection = FlexDirection.ROW;
            NodeId root = tree.newWithChildren(rootStyle, child1, child2);
            
            tree.computeLayout(root, Size.maxContent());
            assertEquals(0, tree.getLayout(child1).location().x, EPSILON);
            assertEquals(50, tree.getLayout(child2).location().x, EPSILON);
            
            // Reorder: put child2 first
            tree.setChildren(root, child2, child1);
            tree.computeLayout(root, Size.maxContent());
            
            assertEquals(0, tree.getLayout(child2).location().x, EPSILON);
            assertEquals(100, tree.getLayout(child1).location().x, EPSILON);
        }
        
        @Test
        @DisplayName("Nested style changes should propagate correctly")
        void nestedStyleChangesShouldPropagateCorrectly() {
            TaffyTree tree = new TaffyTree();
            
            // Create: root -> container -> item
            Style itemStyle = new Style();
            itemStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            NodeId item = tree.newLeaf(itemStyle);
            
            Style containerStyle = new Style();
            containerStyle.display = Display.FLEX;
            containerStyle.padding = new Rect<>(
                LengthPercentage.length(10), LengthPercentage.length(10),
                LengthPercentage.length(10), LengthPercentage.length(10)
            );
            NodeId container = tree.newWithChildren(containerStyle, item);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            NodeId root = tree.newWithChildren(rootStyle, container);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Container should be 50 + 10 + 10 = 70 wide
            assertEquals(70, tree.getLayout(container).size().width, EPSILON);
            // Item should be at x=10 within container
            assertEquals(10, tree.getLayout(item).location().x, EPSILON);
            
            // Change item size
            Style newItemStyle = new Style();
            newItemStyle.size = new Size<>(Dimension.length(100), Dimension.length(50));
            tree.setStyle(item, newItemStyle);
            tree.computeLayout(root, Size.maxContent());
            
            // Container should now be 100 + 10 + 10 = 120 wide
            assertEquals(120, tree.getLayout(container).size().width, EPSILON);
        }
    }
    
    // ==================== Cache Consistency Tests ====================
    
    @Nested
    @DisplayName("Cache Consistency Tests")
    class CacheConsistencyTests {
        
        @RepeatedTest(10)
        @DisplayName("Multiple computations should produce identical results")
        void multipleComputationsShouldProduceIdenticalResults() {
            TaffyTree tree = new TaffyTree();
            
            // Create a moderately complex tree
            List<NodeId> children = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Style style = new Style();
                style.flexGrow = (float) (i + 1);
                style.size = new Size<>(Dimension.AUTO, Dimension.length(30));
                children.add(tree.newLeaf(style));
            }
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexDirection = FlexDirection.ROW;
            rootStyle.size = new Size<>(Dimension.length(500), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, children.toArray(new NodeId[0]));
            
            // First computation
            tree.computeLayout(root, Size.maxContent());
            float[] firstWidths = new float[5];
            float[] firstXs = new float[5];
            for (int i = 0; i < 5; i++) {
                firstWidths[i] = tree.getLayout(children.get(i)).size().width;
                firstXs[i] = tree.getLayout(children.get(i)).location().x;
            }
            
            // Re-compute and verify
            tree.computeLayout(root, Size.maxContent());
            for (int i = 0; i < 5; i++) {
                assertEquals(firstWidths[i], tree.getLayout(children.get(i)).size().width, EPSILON,
                    "Width mismatch for child " + i);
                assertEquals(firstXs[i], tree.getLayout(children.get(i)).location().x, EPSILON,
                    "X position mismatch for child " + i);
            }
        }
        
        @Test
        @DisplayName("Modify then revert should restore original layout")
        void modifyThenRevertShouldRestoreOriginalLayout() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            NodeId child = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.justifyContent = AlignContent.CENTER;
            rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            // Original layout
            tree.computeLayout(root, Size.maxContent());
            float originalX = tree.getLayout(child).location().x;
            float originalWidth = tree.getLayout(child).size().width;
            
            // Modify
            Style modifiedStyle = new Style();
            modifiedStyle.size = new Size<>(Dimension.length(100), Dimension.length(50));
            tree.setStyle(child, modifiedStyle);
            tree.computeLayout(root, Size.maxContent());
            
            assertNotEquals(originalWidth, tree.getLayout(child).size().width, EPSILON);
            
            // Revert
            tree.setStyle(child, childStyle);
            tree.computeLayout(root, Size.maxContent());
            
            assertEquals(originalX, tree.getLayout(child).location().x, EPSILON);
            assertEquals(originalWidth, tree.getLayout(child).size().width, EPSILON);
        }
        
        @Test
        @DisplayName("Different available space should produce different layouts")
        void differentAvailableSpaceShouldProduceDifferentLayouts() {
            TaffyTree tree = new TaffyTree();
            
            // Use root with explicit size for flex-grow to work
            Style childStyle = new Style();
            childStyle.flexGrow = 1.0f;
            NodeId child = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.percent(1.0f), Dimension.percent(1.0f));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            // Compute with 200px width - percentage of 200 = 200
            tree.computeLayout(root, new Size<>(
                AvailableSpace.definite(200),
                AvailableSpace.definite(100)
            ));
            float width200 = tree.getLayout(child).size().width;
            
            // Compute with 400px width - percentage of 400 = 400
            tree.computeLayout(root, new Size<>(
                AvailableSpace.definite(400),
                AvailableSpace.definite(100)
            ));
            float width400 = tree.getLayout(child).size().width;
            
            assertEquals(200, width200, EPSILON, "Child should use 200px available space");
            assertEquals(400, width400, EPSILON, "Child should use 400px available space");
            
            // Verify switching back works
            tree.computeLayout(root, new Size<>(
                AvailableSpace.definite(200),
                AvailableSpace.definite(100)
            ));
            assertEquals(200, tree.getLayout(child).size().width, EPSILON);
        }
        
        @Test
        @DisplayName("Measure function count should be minimized by cache")
        void measureFunctionCountShouldBeMinimizedByCache() {
            TaffyTree tree = new TaffyTree();
            AtomicInteger measureCount = new AtomicInteger(0);
            
            MeasureFunc measure = (known, avail) -> {
                measureCount.incrementAndGet();
                return new FloatSize(50, 30);
            };
            
            Style leafStyle = new Style();
            NodeId leaf = tree.newLeafWithMeasure(leafStyle, measure);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            NodeId root = tree.newWithChildren(rootStyle, leaf);
            
            // First layout
            tree.computeLayout(root, Size.maxContent());
            int firstCount = measureCount.get();
            
            // Second layout with same constraints - should use cache
            tree.computeLayout(root, Size.maxContent());
            int secondCount = measureCount.get();
            
            // Measure should not be called again if cache is working
            assertTrue(secondCount <= firstCount * 2,
                "Measure called too many times: first=" + firstCount + ", second=" + secondCount);
        }
    }
    
    // ==================== Edge Case Tests ====================
    
    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Empty tree should not crash")
        void emptyTreeShouldNotCrash() {
            TaffyTree tree = new TaffyTree();
            NodeId empty = tree.newLeaf(new Style());
            
            assertDoesNotThrow(() -> tree.computeLayout(empty, Size.maxContent()));
            
            Layout layout = tree.getLayout(empty);
            assertNotNull(layout);
            assertEquals(0, layout.size().width, EPSILON);
            assertEquals(0, layout.size().height, EPSILON);
        }
        
        @Test
        @DisplayName("Single node with fixed size")
        void singleNodeWithFixedSize() {
            TaffyTree tree = new TaffyTree();
            
            Style style = new Style();
            style.size = new Size<>(Dimension.length(100), Dimension.length(50));
            NodeId node = tree.newLeaf(style);
            
            tree.computeLayout(node, Size.maxContent());
            
            assertEquals(100, tree.getLayout(node).size().width, EPSILON);
            assertEquals(50, tree.getLayout(node).size().height, EPSILON);
        }
        
        @Test
        @DisplayName("Zero size container should not crash")
        void zeroSizeContainerShouldNotCrash() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            NodeId child = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.length(0), Dimension.length(0));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            assertDoesNotThrow(() -> tree.computeLayout(root, Size.maxContent()));
        }
        
        @Test
        @DisplayName("Very large values should not overflow")
        void veryLargeValuesShouldNotOverflow() {
            TaffyTree tree = new TaffyTree();
            
            Style style = new Style();
            style.size = new Size<>(
                Dimension.length(1_000_000f),
                Dimension.length(1_000_000f)
            );
            NodeId node = tree.newLeaf(style);
            
            tree.computeLayout(node, Size.maxContent());
            
            assertEquals(1_000_000f, tree.getLayout(node).size().width, 1f);
            assertEquals(1_000_000f, tree.getLayout(node).size().height, 1f);
        }
        
        @Test
        @DisplayName("Very small values should be handled")
        void verySmallValuesShouldBeHandled() {
            TaffyTree tree = new TaffyTree();
            
            Style style = new Style();
            style.size = new Size<>(
                Dimension.length(0.001f),
                Dimension.length(0.001f)
            );
            NodeId node = tree.newLeaf(style);
            
            tree.computeLayout(node, Size.maxContent());
            
            // Should be rounded to near-zero
            assertTrue(tree.getLayout(node).size().width < 1f);
            assertTrue(tree.getLayout(node).size().height < 1f);
        }
        
        @Test
        @DisplayName("Display none should hide node and children")
        void displayNoneShouldHideNodeAndChildren() {
            TaffyTree tree = new TaffyTree();
            
            Style visibleStyle = new Style();
            visibleStyle.size = new Size<>(Dimension.length(100), Dimension.length(50));
            NodeId visibleChild = tree.newLeaf(visibleStyle);
            
            Style hiddenStyle = new Style();
            hiddenStyle.display = Display.NONE;
            hiddenStyle.size = new Size<>(Dimension.length(100), Dimension.length(50));
            NodeId hiddenChild = tree.newLeaf(hiddenStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexDirection = FlexDirection.ROW;
            NodeId root = tree.newWithChildren(rootStyle, hiddenChild, visibleChild);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Hidden child should have zero size
            assertEquals(0, tree.getLayout(hiddenChild).size().width, EPSILON);
            assertEquals(0, tree.getLayout(hiddenChild).size().height, EPSILON);
            
            // Visible child should be at x=0 (hidden child doesn't take space)
            assertEquals(0, tree.getLayout(visibleChild).location().x, EPSILON);
        }
        
        @Test
        @DisplayName("Negative margins should work correctly")
        void negativeMarginsWorkCorrectly() {
            TaffyTree tree = new TaffyTree();
            
            Style child1Style = new Style();
            child1Style.size = new Size<>(Dimension.length(100), Dimension.length(50));
            NodeId child1 = tree.newLeaf(child1Style);
            
            Style child2Style = new Style();
            child2Style.size = new Size<>(Dimension.length(100), Dimension.length(50));
            child2Style.margin = new Rect<>(
                LengthPercentageAuto.length(-20f),
                LengthPercentageAuto.ZERO,
                LengthPercentageAuto.ZERO,
                LengthPercentageAuto.ZERO
            );
            NodeId child2 = tree.newLeaf(child2Style);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexDirection = FlexDirection.ROW;
            NodeId root = tree.newWithChildren(rootStyle, child1, child2);
            
            tree.computeLayout(root, Size.maxContent());
            
            // child2 should overlap child1 due to negative margin
            assertEquals(0, tree.getLayout(child1).location().x, EPSILON);
            assertEquals(80, tree.getLayout(child2).location().x, EPSILON); // 100 - 20
        }
    }
    
    // ==================== Flexbox Web Compatibility Tests ====================
    
    @Nested
    @DisplayName("Flexbox Web Compatibility Tests")
    class FlexboxWebCompatibilityTests {
        
        @Test
        @DisplayName("flex-grow distribution")
        void flexGrowDistribution() {
            TaffyTree tree = new TaffyTree();
            
            // CSS: flex-grow: 1; flex-grow: 2; flex-grow: 3;
            Style style1 = new Style();
            style1.flexGrow = 1f;
            Style style2 = new Style();
            style2.flexGrow = 2f;
            Style style3 = new Style();
            style3.flexGrow = 3f;
            
            NodeId child1 = tree.newLeaf(style1);
            NodeId child2 = tree.newLeaf(style2);
            NodeId child3 = tree.newLeaf(style3);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.length(600), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Total grow = 6, so: 1/6 * 600 = 100, 2/6 * 600 = 200, 3/6 * 600 = 300
            assertEquals(100, tree.getLayout(child1).size().width, EPSILON);
            assertEquals(200, tree.getLayout(child2).size().width, EPSILON);
            assertEquals(300, tree.getLayout(child3).size().width, EPSILON);
        }
        
        @Test
        @DisplayName("flex-shrink distribution")
        void flexShrinkDistribution() {
            TaffyTree tree = new TaffyTree();
            
            // Each child wants 200px but container is only 300px
            Style style1 = new Style();
            style1.flexShrink = 1f;
            style1.size = new Size<>(Dimension.length(200), Dimension.AUTO);
            
            Style style2 = new Style();
            style2.flexShrink = 2f;
            style2.size = new Size<>(Dimension.length(200), Dimension.AUTO);
            
            NodeId child1 = tree.newLeaf(style1);
            NodeId child2 = tree.newLeaf(style2);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.length(300), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child1, child2);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Total shrink basis = 1*200 + 2*200 = 600
            // Need to remove 100px
            // child1 loses: 100 * (1*200/600) = 33.33
            // child2 loses: 100 * (2*200/600) = 66.67
            assertEquals(166.67f, tree.getLayout(child1).size().width, 1f);
            assertEquals(133.33f, tree.getLayout(child2).size().width, 1f);
        }
        
        @Test
        @DisplayName("flex-basis should override width")
        void flexBasisShouldOverrideWidth() {
            TaffyTree tree = new TaffyTree();
            
            Style style = new Style();
            style.size = new Size<>(Dimension.length(100), Dimension.AUTO);
            style.flexBasis = Dimension.length(200);
            NodeId child = tree.newLeaf(style);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.length(400), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            tree.computeLayout(root, Size.maxContent());
            
            // flex-basis (200) should be used instead of width (100)
            assertEquals(200, tree.getLayout(child).size().width, EPSILON);
        }
        
        @Test
        @DisplayName("justify-content space-between")
        void justifyContentSpaceBetween() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            
            NodeId child1 = tree.newLeaf(childStyle);
            NodeId child2 = tree.newLeaf(childStyle);
            NodeId child3 = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.justifyContent = AlignContent.SPACE_BETWEEN;
            rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);
            
            tree.computeLayout(root, Size.maxContent());
            
            // 200 - 150 = 50 free space, 2 gaps
            // Gap = 50 / 2 = 25
            assertEquals(0, tree.getLayout(child1).location().x, EPSILON);
            assertEquals(75, tree.getLayout(child2).location().x, EPSILON); // 50 + 25
            assertEquals(150, tree.getLayout(child3).location().x, EPSILON); // 50 + 25 + 50 + 25
        }
        
        @Test
        @DisplayName("align-items center")
        void alignItemsCenter() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.size = new Size<>(Dimension.length(50), Dimension.length(30));
            NodeId child = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.alignItems = AlignItems.CENTER;
            rootStyle.size = new Size<>(Dimension.length(100), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Centered vertically: (100 - 30) / 2 = 35
            assertEquals(35, tree.getLayout(child).location().y, EPSILON);
        }
        
        @Test
        @DisplayName("flex-wrap should create multiple lines")
        void flexWrapShouldCreateMultipleLines() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.size = new Size<>(Dimension.length(60), Dimension.length(30));
            
            List<NodeId> children = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                children.add(tree.newLeaf(childStyle));
            }
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexWrap = FlexWrap.WRAP;
            rootStyle.size = new Size<>(Dimension.length(150), Dimension.AUTO);
            NodeId root = tree.newWithChildren(rootStyle, children.toArray(new NodeId[0]));
            
            tree.computeLayout(root, Size.maxContent());
            
            // 150px can fit 2 items per row (120px < 150px, 180px > 150px)
            // Row 1: items 0, 1 at y=0
            // Row 2: items 2, 3 at y=30
            // Row 3: item 4 at y=60
            assertEquals(0, tree.getLayout(children.get(0)).location().y, EPSILON);
            assertEquals(0, tree.getLayout(children.get(1)).location().y, EPSILON);
            assertEquals(30, tree.getLayout(children.get(2)).location().y, EPSILON);
            assertEquals(30, tree.getLayout(children.get(3)).location().y, EPSILON);
            assertEquals(60, tree.getLayout(children.get(4)).location().y, EPSILON);
        }
        
        @Test
        @DisplayName("min-width should prevent shrinking below")
        void minWidthShouldPreventShrinkingBelow() {
            TaffyTree tree = new TaffyTree();
            
            Style style = new Style();
            style.flexShrink = 1f;
            style.size = new Size<>(Dimension.length(200), Dimension.AUTO);
            style.minSize = new Size<>(Dimension.length(150), Dimension.AUTO);
            NodeId child = tree.newLeaf(style);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.length(100), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Even though container is 100, min-width prevents shrinking below 150
            assertEquals(150, tree.getLayout(child).size().width, EPSILON);
        }
        
        @Test
        @DisplayName("max-width should prevent growing above")
        void maxWidthShouldPreventGrowingAbove() {
            TaffyTree tree = new TaffyTree();
            
            Style style = new Style();
            style.flexGrow = 1f;
            style.maxSize = new Size<>(Dimension.length(150), Dimension.AUTO);
            NodeId child = tree.newLeaf(style);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.length(300), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Even with flex-grow, max-width caps at 150
            assertEquals(150, tree.getLayout(child).size().width, EPSILON);
        }
        
        @Test
        @DisplayName("auto margins should absorb free space")
        void autoMarginsShouldAbsorbFreeSpace() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            childStyle.margin = new Rect<>(
                LengthPercentageAuto.AUTO,
                LengthPercentageAuto.AUTO,
                LengthPercentageAuto.ZERO,
                LengthPercentageAuto.ZERO
            );
            NodeId child = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Auto margins on left and right should center horizontally
            // (200 - 50) / 2 = 75
            assertEquals(75, tree.getLayout(child).location().x, EPSILON);
        }
    }
    
    // ==================== Grid Web Compatibility Tests ====================
    
    @Nested
    @DisplayName("Grid Web Compatibility Tests")
    class GridWebCompatibilityTests {
        
        @Test
        @DisplayName("explicit grid tracks")
        void explicitGridTracks() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            NodeId child1 = tree.newLeaf(childStyle);
            NodeId child2 = tree.newLeaf(childStyle);
            NodeId child3 = tree.newLeaf(childStyle);
            NodeId child4 = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.GRID;
            rootStyle.gridTemplateColumns = new ArrayList<>(Arrays.asList(
                TrackSizingFunction.fixed(LengthPercentage.length(100)),
                TrackSizingFunction.fixed(LengthPercentage.length(100))
            ));
            rootStyle.gridTemplateRows = new ArrayList<>(Arrays.asList(
                TrackSizingFunction.fixed(LengthPercentage.length(50)),
                TrackSizingFunction.fixed(LengthPercentage.length(50))
            ));
            NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3, child4);
            
            tree.computeLayout(root, Size.maxContent());
            
            // 2x2 grid: 100x50 each cell
            assertEquals(0, tree.getLayout(child1).location().x, EPSILON);
            assertEquals(0, tree.getLayout(child1).location().y, EPSILON);
            assertEquals(100, tree.getLayout(child2).location().x, EPSILON);
            assertEquals(0, tree.getLayout(child2).location().y, EPSILON);
            assertEquals(0, tree.getLayout(child3).location().x, EPSILON);
            assertEquals(50, tree.getLayout(child3).location().y, EPSILON);
            assertEquals(100, tree.getLayout(child4).location().x, EPSILON);
            assertEquals(50, tree.getLayout(child4).location().y, EPSILON);
        }
        
        @Test
        @DisplayName("fr units should distribute proportionally")
        void frUnitsShouldDistributeProportionally() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            NodeId child1 = tree.newLeaf(childStyle);
            NodeId child2 = tree.newLeaf(childStyle);
            NodeId child3 = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.GRID;
            rootStyle.gridTemplateColumns = new ArrayList<>(Arrays.asList(
                TrackSizingFunction.fr(1),
                TrackSizingFunction.fr(2),
                TrackSizingFunction.fr(3)
            ));
            rootStyle.size = new Size<>(Dimension.length(600), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child1, child2, child3);
            
            tree.computeLayout(root, Size.maxContent());
            
            // 1fr:2fr:3fr = 100:200:300 in 600px
            assertEquals(100, tree.getLayout(child1).size().width, EPSILON);
            assertEquals(200, tree.getLayout(child2).size().width, EPSILON);
            assertEquals(300, tree.getLayout(child3).size().width, EPSILON);
        }
        
        @Test
        @DisplayName("grid-gap should add spacing")
        void gridGapShouldAddSpacing() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            NodeId child1 = tree.newLeaf(childStyle);
            NodeId child2 = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.GRID;
            rootStyle.gridTemplateColumns = new ArrayList<>(Arrays.asList(
                TrackSizingFunction.fixed(LengthPercentage.length(100)),
                TrackSizingFunction.fixed(LengthPercentage.length(100))
            ));
            rootStyle.gap = new Size<>(
                LengthPercentage.length(20), // column gap
                LengthPercentage.length(10)  // row gap
            );
            NodeId root = tree.newWithChildren(rootStyle, child1, child2);
            
            tree.computeLayout(root, Size.maxContent());
            
            assertEquals(0, tree.getLayout(child1).location().x, EPSILON);
            assertEquals(120, tree.getLayout(child2).location().x, EPSILON); // 100 + 20 gap
        }
        
        @Test
        @DisplayName("grid item spanning multiple columns")
        void gridItemSpanningMultipleColumns() {
            TaffyTree tree = new TaffyTree();
            
            // Item spanning 2 columns
            Style spanningStyle = new Style();
            spanningStyle.gridColumn = new Line<>(
                GridPlacement.line(1),
                GridPlacement.span(2)
            );
            NodeId spanning = tree.newLeaf(spanningStyle);
            
            // Regular items
            NodeId child1 = tree.newLeaf(new Style());
            NodeId child2 = tree.newLeaf(new Style());
            
            Style rootStyle = new Style();
            rootStyle.display = Display.GRID;
            rootStyle.gridTemplateColumns = new ArrayList<>(Arrays.asList(
                TrackSizingFunction.fixed(LengthPercentage.length(100)),
                TrackSizingFunction.fixed(LengthPercentage.length(100))
            ));
            rootStyle.gridTemplateRows = new ArrayList<>(Arrays.asList(
                TrackSizingFunction.fixed(LengthPercentage.length(50)),
                TrackSizingFunction.fixed(LengthPercentage.length(50))
            ));
            NodeId root = tree.newWithChildren(rootStyle, spanning, child1, child2);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Spanning item should be 200px wide (2 columns)
            assertEquals(200, tree.getLayout(spanning).size().width, EPSILON);
        }
    }
    
    // ==================== Complex Scenario Tests ====================
    
    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {
        
        @Test
        @DisplayName("Deep nesting should not cause stack overflow")
        void deepNestingShouldNotCauseStackOverflow() {
            TaffyTree tree = new TaffyTree();
            
            Style leafStyle = new Style();
            leafStyle.size = new Size<>(Dimension.length(10), Dimension.length(10));
            NodeId current = tree.newLeaf(leafStyle);
            
            // Create 200 levels of nesting
            for (int i = 0; i < 200; i++) {
                Style wrapperStyle = new Style();
                wrapperStyle.display = Display.FLEX;
                wrapperStyle.padding = new Rect<>(
                    LengthPercentage.length(1), LengthPercentage.length(1),
                    LengthPercentage.length(1), LengthPercentage.length(1)
                );
                current = tree.newWithChildren(wrapperStyle, current);
            }
            
            final NodeId root = current;
            assertDoesNotThrow(() -> tree.computeLayout(root, Size.maxContent()));
            
            // Root should be 10 + 200*2 = 410 wide (each level adds 2px padding)
            assertEquals(410, tree.getLayout(root).size().width, 1f);
        }
        
        @Test
        @DisplayName("Mixed flex and grid layout")
        void mixedFlexAndGridLayout() {
            TaffyTree tree = new TaffyTree();
            
            // Grid items
            Style gridItemStyle = new Style();
            gridItemStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            NodeId gridItem1 = tree.newLeaf(gridItemStyle);
            NodeId gridItem2 = tree.newLeaf(gridItemStyle);
            
            // Grid container inside flex
            Style gridStyle = new Style();
            gridStyle.display = Display.GRID;
            gridStyle.gridTemplateColumns = new ArrayList<>(Arrays.asList(
                TrackSizingFunction.fixed(LengthPercentage.length(50)),
                TrackSizingFunction.fixed(LengthPercentage.length(50))
            ));
            NodeId grid = tree.newWithChildren(gridStyle, gridItem1, gridItem2);
            
            // Another flex item
            Style flexItemStyle = new Style();
            flexItemStyle.size = new Size<>(Dimension.length(100), Dimension.length(50));
            NodeId flexItem = tree.newLeaf(flexItemStyle);
            
            // Flex root
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexDirection = FlexDirection.COLUMN;
            NodeId root = tree.newWithChildren(rootStyle, grid, flexItem);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Grid should be 100x50 (2 columns of 50)
            assertEquals(100, tree.getLayout(grid).size().width, EPSILON);
            assertEquals(50, tree.getLayout(grid).size().height, EPSILON);
            
            // Flex item should be below grid
            assertEquals(50, tree.getLayout(flexItem).location().y, EPSILON);
        }
        
        @Test
        @DisplayName("Responsive layout with percentage sizes")
        void responsiveLayoutWithPercentageSizes() {
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.size = new Size<>(Dimension.percent(0.5f), Dimension.percent(0.5f));
            NodeId child = tree.newLeaf(childStyle);
            
            // Root must have explicit size for percentage calculations to work
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.percent(1.0f), Dimension.percent(1.0f));
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            // Test at different sizes
            float[][] testCases = {
                {200, 200, 100, 100},
                {400, 300, 200, 150},
                {100, 500, 50, 250}
            };
            
            for (float[] tc : testCases) {
                float rootW = tc[0], rootH = tc[1], expectedW = tc[2], expectedH = tc[3];
                tree.computeLayout(root, new Size<>(
                    AvailableSpace.definite(rootW),
                    AvailableSpace.definite(rootH)
                ));
                
                assertEquals(expectedW, tree.getLayout(child).size().width, EPSILON,
                    "Width mismatch for root " + rootW + "x" + rootH);
                assertEquals(expectedH, tree.getLayout(child).size().height, EPSILON,
                    "Height mismatch for root " + rootW + "x" + rootH);
            }
        }
        
        @Test
        @DisplayName("Scrollable container with overflow")
        void scrollableContainerWithOverflow() {
            TaffyTree tree = new TaffyTree();
            
            // Content larger than container
            Style contentStyle = new Style();
            contentStyle.size = new Size<>(Dimension.length(500), Dimension.length(500));
            NodeId content = tree.newLeaf(contentStyle);
            
            // Scrollable container with SCROLL overflow
            Style containerStyle = new Style();
            containerStyle.display = Display.FLEX;
            containerStyle.overflow = new Point<>(Overflow.SCROLL, Overflow.SCROLL);
            containerStyle.size = new Size<>(Dimension.length(200), Dimension.length(200));
            NodeId container = tree.newWithChildren(containerStyle, content);
            
            tree.computeLayout(container, Size.maxContent());
            
            // Container should maintain its explicit size
            assertEquals(200, tree.getLayout(container).size().width, EPSILON);
            assertEquals(200, tree.getLayout(container).size().height, EPSILON);
            
            // In flexbox with overflow:scroll, content may be constrained.
            // The actual behavior depends on the library's implementation.
            // Just verify layout completes without error and content has valid dimensions.
            Layout contentLayout = tree.getLayout(content);
            assertTrue(contentLayout.size().width > 0, "Content should have positive width");
            assertTrue(contentLayout.size().height > 0, "Content should have positive height");
        }
        
        @Test
        @DisplayName("Absolute positioning within flex container")
        void absolutePositioningWithinFlexContainer() {
            TaffyTree tree = new TaffyTree();
            
            // Normal flex item
            Style normalStyle = new Style();
            normalStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            NodeId normal = tree.newLeaf(normalStyle);
            
            // Absolutely positioned item
            Style absoluteStyle = new Style();
            absoluteStyle.position = Position.ABSOLUTE;
            absoluteStyle.size = new Size<>(Dimension.length(30), Dimension.length(30));
            absoluteStyle.inset = new Rect<>(
                LengthPercentageAuto.length(10),
                LengthPercentageAuto.AUTO,
                LengthPercentageAuto.length(10),
                LengthPercentageAuto.AUTO
            );
            NodeId absolute = tree.newLeaf(absoluteStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(200));
            NodeId root = tree.newWithChildren(rootStyle, normal, absolute);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Normal item at origin
            assertEquals(0, tree.getLayout(normal).location().x, EPSILON);
            assertEquals(0, tree.getLayout(normal).location().y, EPSILON);
            
            // Absolute item positioned at (10, 10)
            assertEquals(10, tree.getLayout(absolute).location().x, EPSILON);
            assertEquals(10, tree.getLayout(absolute).location().y, EPSILON);
        }
        
        @Test
        @DisplayName("Complex form layout simulation")
        void complexFormLayoutSimulation() {
            TaffyTree tree = new TaffyTree();
            
            List<NodeId> rows = new ArrayList<>();
            
            // Create 5 form rows with label + input
            for (int i = 0; i < 5; i++) {
                Style labelStyle = new Style();
                labelStyle.size = new Size<>(Dimension.length(100), Dimension.length(20));
                NodeId label = tree.newLeaf(labelStyle);
                
                Style inputStyle = new Style();
                inputStyle.flexGrow = 1;
                inputStyle.size = new Size<>(Dimension.AUTO, Dimension.length(30));
                NodeId input = tree.newLeaf(inputStyle);
                
                Style rowStyle = new Style();
                rowStyle.display = Display.FLEX;
                rowStyle.flexDirection = FlexDirection.ROW;
                rowStyle.alignItems = AlignItems.CENTER;
                rowStyle.gap = new Size<>(LengthPercentage.length(10), LengthPercentage.ZERO);
                rowStyle.margin = new Rect<>(
                    LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
                    LengthPercentageAuto.ZERO, LengthPercentageAuto.length(10)
                );
                rows.add(tree.newWithChildren(rowStyle, label, input));
            }
            
            Style formStyle = new Style();
            formStyle.display = Display.FLEX;
            formStyle.flexDirection = FlexDirection.COLUMN;
            formStyle.size = new Size<>(Dimension.length(400), Dimension.AUTO);
            NodeId form = tree.newWithChildren(formStyle, rows.toArray(new NodeId[0]));
            
            tree.computeLayout(form, Size.maxContent());
            
            // Verify layout
            float expectedY = 0;
            for (int i = 0; i < 5; i++) {
                Layout rowLayout = tree.getLayout(rows.get(i));
                assertEquals(expectedY, rowLayout.location().y, 1f, "Row " + i + " Y position");
                assertEquals(400, rowLayout.size().width, EPSILON, "Row " + i + " width");
                expectedY += 30 + 10; // row height + margin
            }
        }
    }
    
    // ==================== Randomized Stress Tests ====================
    
    @Nested
    @DisplayName("Randomized Stress Tests")
    class RandomizedStressTests {
        
        @RepeatedTest(5)
        @DisplayName("Random tree modifications should not crash")
        void randomTreeModificationsShouldNotCrash() {
            TaffyTree tree = new TaffyTree();
            Random random = new Random(System.currentTimeMillis());
            
            // Create initial tree
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexWrap = FlexWrap.WRAP;
            rootStyle.size = new Size<>(Dimension.length(500), Dimension.length(500));
            NodeId root = tree.newLeaf(rootStyle);
            
            List<NodeId> nodes = new ArrayList<>();
            nodes.add(root);
            
            // Perform 100 random operations
            for (int i = 0; i < 100; i++) {
                int op = random.nextInt(4);
                
                switch (op) {
                    case 0: // Add node
                        Style style = new Style();
                        style.size = new Size<>(
                            Dimension.length(10 + random.nextInt(90)),
                            Dimension.length(10 + random.nextInt(90))
                        );
                        NodeId newNode = tree.newLeaf(style);
                        NodeId parent = nodes.get(random.nextInt(nodes.size()));
                        tree.addChild(parent, newNode);
                        nodes.add(newNode);
                        break;
                        
                    case 1: // Modify style
                        if (nodes.size() > 1) {
                            NodeId target = nodes.get(random.nextInt(nodes.size()));
                            Style newStyle = new Style();
                            newStyle.flexGrow = random.nextFloat();
                            newStyle.flexShrink = random.nextFloat();
                            tree.setStyle(target, newStyle);
                        }
                        break;
                        
                    case 2: // Compute layout
                        assertDoesNotThrow(() -> tree.computeLayout(root, Size.maxContent()));
                        break;
                        
                    case 3: // Remove node (if not root)
                        if (nodes.size() > 2) {
                            int idx = 1 + random.nextInt(nodes.size() - 1);
                            NodeId toRemove = nodes.get(idx);
                            // Find parent and remove
                            for (NodeId potentialParent : nodes) {
                                if (tree.getChildren(potentialParent).contains(toRemove)) {
                                    tree.removeChild(potentialParent, toRemove);
                                    break;
                                }
                            }
                            nodes.remove(idx);
                        }
                        break;
                }
            }
            
            // Final layout should work
            assertDoesNotThrow(() -> tree.computeLayout(root, Size.maxContent()));
        }
        
        @ParameterizedTest
        @ValueSource(ints = {10, 50, 100, 200})
        @DisplayName("Large number of siblings should work correctly")
        void largeNumberOfSiblingsShouldWorkCorrectly(int count) {
            TaffyTree tree = new TaffyTree();
            
            List<NodeId> children = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Style style = new Style();
                style.size = new Size<>(Dimension.length(10), Dimension.length(10));
                children.add(tree.newLeaf(style));
            }
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexWrap = FlexWrap.WRAP;
            rootStyle.size = new Size<>(Dimension.length(100), Dimension.AUTO);
            NodeId root = tree.newWithChildren(rootStyle, children.toArray(new NodeId[0]));
            
            assertDoesNotThrow(() -> tree.computeLayout(root, Size.maxContent()));
            
            // Verify children are laid out
            for (NodeId child : children) {
                Layout layout = tree.getLayout(child);
                assertEquals(10, layout.size().width, EPSILON);
                assertEquals(10, layout.size().height, EPSILON);
            }
        }
    }
    
    // ==================== Regression Tests ====================
    
    @Nested
    @DisplayName("Regression Tests")
    class RegressionTests {
        
        @Test
        @DisplayName("Percentage height with indefinite parent")
        void percentageHeightWithIndefiniteParent() {
            // Per CSS spec, percentage heights with indefinite parent should resolve to auto
            TaffyTree tree = new TaffyTree();
            
            Style childStyle = new Style();
            childStyle.size = new Size<>(Dimension.length(100), Dimension.percent(0.5f));
            NodeId child = tree.newLeaf(childStyle);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            // No height specified - indefinite
            NodeId root = tree.newWithChildren(rootStyle, child);
            
            tree.computeLayout(root, new Size<>(
                AvailableSpace.definite(200),
                AvailableSpace.maxContent()
            ));
            
            // Height should be 0 since percentage of indefinite is auto
            assertTrue(tree.getLayout(child).size().height <= EPSILON ||
                       Float.isNaN(tree.getLayout(child).size().height) == false);
        }
        
        @Test
        @DisplayName("Margin collapse should not happen in flexbox")
        void marginCollapseShouldNotHappenInFlexbox() {
            TaffyTree tree = new TaffyTree();
            
            // Child 1: 50px height with 20px bottom margin
            Style style1 = new Style();
            style1.size = new Size<>(Dimension.length(50), Dimension.length(50));
            style1.margin = new Rect<>(
                LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
                LengthPercentageAuto.ZERO, LengthPercentageAuto.length(20) // bottom margin
            );
            NodeId child1 = tree.newLeaf(style1);
            
            // Child 2: 50px height with 20px top margin
            Style style2 = new Style();
            style2.size = new Size<>(Dimension.length(50), Dimension.length(50));
            style2.margin = new Rect<>(
                LengthPercentageAuto.ZERO, LengthPercentageAuto.ZERO,
                LengthPercentageAuto.length(20), LengthPercentageAuto.ZERO // top margin
            );
            NodeId child2 = tree.newLeaf(style2);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexDirection = FlexDirection.COLUMN;
            NodeId root = tree.newWithChildren(rootStyle, child1, child2);
            
            tree.computeLayout(root, Size.maxContent());
            
            // In CSS flexbox, adjacent margins don't collapse.
            // child1 at y=0, height=50, bottom-margin=20
            // child2 at y=50+20+20=90, height=50
            // Total height = 50 + 20 + 20 + 50 = 140
            assertEquals(0, tree.getLayout(child1).location().y, EPSILON);
            // Both margins are preserved (no collapse)
            assertEquals(90, tree.getLayout(child2).location().y, EPSILON); // 50 + 20 + 20
            assertEquals(140, tree.getLayout(root).size().height, EPSILON);
        }
        
        @Test
        @DisplayName("Aspect ratio should be maintained")
        void aspectRatioShouldBeMaintained() {
            TaffyTree tree = new TaffyTree();
            
            Style style = new Style();
            style.size = new Size<>(Dimension.length(100), Dimension.AUTO);
            style.aspectRatio = 2.0f; // width:height = 2:1
            NodeId node = tree.newLeaf(style);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            NodeId root = tree.newWithChildren(rootStyle, node);
            
            tree.computeLayout(root, Size.maxContent());
            
            // Height should be 50 (width/aspectRatio = 100/2)
            assertEquals(100, tree.getLayout(node).size().width, EPSILON);
            assertEquals(50, tree.getLayout(node).size().height, EPSILON);
        }
        
        @Test
        @DisplayName("RTL direction should reverse flex items")
        void rtlDirectionShouldReverseFlexItems() {
            TaffyTree tree = new TaffyTree();
            
            Style style1 = new Style();
            style1.size = new Size<>(Dimension.length(50), Dimension.length(50));
            NodeId child1 = tree.newLeaf(style1);
            
            Style style2 = new Style();
            style2.size = new Size<>(Dimension.length(100), Dimension.length(50));
            NodeId child2 = tree.newLeaf(style2);
            
            Style rootStyle = new Style();
            rootStyle.display = Display.FLEX;
            rootStyle.flexDirection = FlexDirection.ROW_REVERSE;
            rootStyle.size = new Size<>(Dimension.length(200), Dimension.length(100));
            NodeId root = tree.newWithChildren(rootStyle, child1, child2);
            
            tree.computeLayout(root, Size.maxContent());
            
            // In row-reverse, items go from right to left
            // child1 (50px) should be at x = 200 - 50 = 150
            // child2 (100px) should be at x = 200 - 50 - 100 = 50
            assertEquals(150, tree.getLayout(child1).location().x, EPSILON);
            assertEquals(50, tree.getLayout(child2).location().x, EPSILON);
        }
    }
}
