package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import dev.vfyjxf.taffy.geometry.Rect;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmarks for TaffyTree layout performance.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class TaffyBenchmark {

    private TaffyTree tree;
    private NodeId rootNode;

    @Param({"10", "100", "1000"})
    private int nodeCount;

    @Setup(Level.Trial)
    public void setup() {
        tree = new TaffyTree();
        rootNode = createWideTree(nodeCount);
    }

    /**
     * Creates a wide tree with the specified number of leaf nodes.
     */
    private NodeId createWideTree(int leafCount) {
        TaffyTree tree = this.tree;
        
        Style leafStyle = new Style();
        leafStyle.size = new Size<>(Dimension.length(100), Dimension.length(100));
        leafStyle.flexGrow = 1.0f;
        
        NodeId[] children = new NodeId[leafCount];
        for (int i = 0; i < leafCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        
        Style parentStyle = new Style();
        parentStyle.display = Display.FLEX;
        parentStyle.flexDirection = FlexDirection.ROW;
        parentStyle.flexWrap = FlexWrap.WRAP;
        parentStyle.size = new Size<>(Dimension.length(1000), Dimension.AUTO);
        
        return tree.newWithChildren(parentStyle, children);
    }

    @Benchmark
    public void benchmarkLayoutComputation() {
        tree.markDirty(rootNode);
        tree.computeLayout(rootNode, new Size<>(
            AvailableSpace.definite(1000f),
            AvailableSpace.definite(10000f)
        ));
    }

    @State(Scope.Thread)
    public static class DeepTreeState {
        TaffyTree tree;
        NodeId rootNode;

        @Param({"5", "10", "15"})
        int depth;

        @Setup(Level.Trial)
        public void setup() {
            tree = new TaffyTree();
            rootNode = createDeepTree(depth);
        }

        private NodeId createDeepTree(int depth) {
            Style leafStyle = new Style();
            leafStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            
            NodeId currentNode = tree.newLeaf(leafStyle);
            
            for (int i = 1; i < depth; i++) {
                Style parentStyle = new Style();
                parentStyle.display = Display.FLEX;
                parentStyle.flexDirection = FlexDirection.COLUMN;
                parentStyle.padding = Rect.all(LengthPercentage.length(10f));
                
                currentNode = tree.newWithChildren(parentStyle, currentNode);
            }
            
            return currentNode;
        }
    }

    @Benchmark
    public void benchmarkDeepTreeLayout(DeepTreeState state) {
        state.tree.markDirty(state.rootNode);
        state.tree.computeLayout(state.rootNode, new Size<>(
            AvailableSpace.maxContent(),
            AvailableSpace.maxContent()
        ));
    }

    @State(Scope.Thread)
    public static class GridState {
        TaffyTree tree;
        NodeId rootNode;

        @Param({"3", "5", "10"})
        int gridSize;

        @Setup(Level.Trial)
        public void setup() {
            tree = new TaffyTree();
            rootNode = createGrid(gridSize);
        }

        private NodeId createGrid(int size) {
            int totalCells = size * size;
            
            Style cellStyle = new Style();
            cellStyle.size = new Size<>(Dimension.length(50), Dimension.length(50));
            
            NodeId[] cells = new NodeId[totalCells];
            for (int i = 0; i < totalCells; i++) {
                cells[i] = tree.newLeaf(cellStyle);
            }
            
            Style gridStyle = new Style();
            gridStyle.display = Display.GRID;
            gridStyle.gap = new Size<>(LengthPercentage.length(10f), LengthPercentage.length(10f));
            
            // Add column template
            for (int i = 0; i < size; i++) {
                gridStyle.gridTemplateColumns.add(TrackSizingFunction.fr(1f));
            }
            
            return tree.newWithChildren(gridStyle, cells);
        }
    }

    @Benchmark
    public void benchmarkGridLayout(GridState state) {
        state.tree.markDirty(state.rootNode);
        state.tree.computeLayout(state.rootNode, new Size<>(
            AvailableSpace.definite(1000f),
            AvailableSpace.definite(1000f)
        ));
    }

    @State(Scope.Thread)
    public static class TreeCreationState {
        @Param({"100", "1000", "10000"})
        int nodeCount;
    }

    @Benchmark
    public TaffyTree benchmarkTreeCreation(TreeCreationState state) {
        TaffyTree tree = new TaffyTree();
        
        Style leafStyle = new Style();
        leafStyle.size = new Size<>(Dimension.length(100), Dimension.length(100));
        
        for (int i = 0; i < state.nodeCount; i++) {
            tree.newLeaf(leafStyle);
        }
        
        return tree;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(TaffyBenchmark.class.getSimpleName())
            .forks(1)
            .build();

        new Runner(opt).run();
    }
}
