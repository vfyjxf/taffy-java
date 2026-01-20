package dev.vfyjxf.taffy;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Diagnostic test to understand caching behavior
 */
public class CacheDiagnosticTest {

    @Test
    @DisplayName("simple_single_node_measure")
    void simpleSingleNodeMeasure() {
        TaffyTree tree = new TaffyTree();
        
        AtomicInteger measureCount = new AtomicInteger(0);
        
        MeasureFunc countingMeasure = (knownDimensions, availableSpace) -> {
            int count = measureCount.incrementAndGet();
            System.out.println("Measure called: count=" + count + 
                ", knownDimensions=" + knownDimensions + 
                ", availableSpace=" + availableSpace);
            return new FloatSize(50.0f, 50.0f);
        };
        
        TaffyStyle leafStyle = new TaffyStyle();
        NodeId leaf = tree.newLeafWithMeasure(leafStyle, countingMeasure);
        
        System.out.println("=== First computeLayout ===");
        tree.computeLayout(leaf, TaffySize.maxContent());
        int firstCount = measureCount.get();
        System.out.println("After first layout: measureCount=" + firstCount);
        
        // Second layout without changes should hit cache
        System.out.println("=== Second computeLayout (same params, should hit cache) ===");
        tree.computeLayout(leaf, TaffySize.maxContent());
        int secondCount = measureCount.get();
        System.out.println("After second layout: measureCount=" + secondCount);
        
        assertEquals(firstCount, secondCount, 
            "Measure should not be called again for identical layout");
    }

    @Test
    @DisplayName("one_level_nesting_measure")
    void oneLevelNestingMeasure() {
        TaffyTree tree = new TaffyTree();
        
        AtomicInteger measureCount = new AtomicInteger(0);
        
        MeasureFunc countingMeasure = (knownDimensions, availableSpace) -> {
            int count = measureCount.incrementAndGet();
            System.out.println("Measure#" + count + ": known=" + knownDimensions + ", avail=" + availableSpace);
            return new FloatSize(50.0f, 50.0f);
        };
        
        TaffyStyle leafStyle = new TaffyStyle();
        NodeId leaf = tree.newLeafWithMeasure(leafStyle, countingMeasure);
        
        // One parent wrapping the leaf
        TaffyStyle parentStyle = new TaffyStyle();
        NodeId parent = tree.newWithChildren(parentStyle, leaf);
        
        System.out.println("=== First computeLayout with one level nesting ===");
        tree.computeLayout(parent, TaffySize.maxContent());
        int firstCount = measureCount.get();
        System.out.println("After first layout: measureCount=" + firstCount);
        
        // Ideally measure should be called <= 4 times (for different sizing modes)
        assertTrue(firstCount <= 10, 
            "Measure called " + firstCount + " times for single nested node, expected <= 10");
    }

    @Test  
    @DisplayName("two_level_nesting_measure")
    void twoLevelNestingMeasure() {
        TaffyTree tree = new TaffyTree();
        
        AtomicInteger measureCount = new AtomicInteger(0);
        
        MeasureFunc countingMeasure = (knownDimensions, availableSpace) -> {
            measureCount.incrementAndGet();
            return new FloatSize(50.0f, 50.0f);
        };
        
        TaffyStyle leafStyle = new TaffyStyle();
        NodeId leaf = tree.newLeafWithMeasure(leafStyle, countingMeasure);
        
        // Two levels of nesting
        NodeId node = tree.newWithChildren(new TaffyStyle(), leaf);
        node = tree.newWithChildren(new TaffyStyle(), node);
        
        System.out.println("=== computeLayout with two level nesting ===");
        tree.computeLayout(node, TaffySize.maxContent());
        int count = measureCount.get();
        System.out.println("After layout: measureCount=" + count);
        
        assertTrue(count <= 20, 
            "Measure called " + count + " times for two level nesting, expected <= 20");
    }

    @Test  
    @DisplayName("three_level_nesting_measure")
    void threeLevelNestingMeasure() {
        TaffyTree tree = new TaffyTree();
        
        AtomicInteger measureCount = new AtomicInteger(0);
        
        MeasureFunc countingMeasure = (knownDimensions, availableSpace) -> {
            measureCount.incrementAndGet();
            return new FloatSize(50.0f, 50.0f);
        };
        
        TaffyStyle leafStyle = new TaffyStyle();
        NodeId leaf = tree.newLeafWithMeasure(leafStyle, countingMeasure);
        
        // Three levels of nesting
        NodeId node = tree.newWithChildren(new TaffyStyle(), leaf);
        node = tree.newWithChildren(new TaffyStyle(), node);
        node = tree.newWithChildren(new TaffyStyle(), node);
        
        System.out.println("=== computeLayout with three level nesting ===");
        tree.computeLayout(node, TaffySize.maxContent());
        int count = measureCount.get();
        System.out.println("After layout: measureCount=" + count);
        
        assertTrue(count <= 50, 
            "Measure called " + count + " times for three level nesting, expected <= 50");
    }
}
