package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Large PanGUI workloads, excluded from the default {@code jmh} task by the
 * {@code .*ExtendedBenchmark.*} exclude pattern and run with {@code ./gradlew jmhExtended}.
 *
 * <p>These are the 100,000-node variants that would dominate a default smoke run:
 * {@code wide_no_wrap_simple_many} (100k leaves) and {@code fit_nesting} (~101k leaves in a
 * 10x10x10x100 nested flex tree).</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class PanguiExtendedBenchmark {

    // === wide_no_wrap_simple_many ===

    @State(Scope.Thread)
    public static class WideNoWrapManyState {
        @Param({"100000"})
        public int nodeCount;

        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = wideNoWrap(nodeCount);
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void wideNoWrapSimpleMany(WideNoWrapManyState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(100f),
            AvailableSpace.definite(100f)
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // === fit_nesting ===

    @State(Scope.Thread)
    public static class FitNestingState {
        BenchSupport.TreeCase treeCase;

        @Setup(Level.Trial)
        public void setupTrial() {
            treeCase = fitNesting();
        }

        @Setup(Level.Invocation)
        public void invalidate() {
            treeCase.invalidateCaches();
        }
    }

    @Benchmark
    public void fitNesting(FitNestingState state, Blackhole bh) {
        state.treeCase.tree.computeLayout(state.treeCase.root, TaffySize.of(
            AvailableSpace.definite(1000f),
            AvailableSpace.maxContent()
        ));
        bh.consume(state.treeCase.tree.getLayout(state.treeCase.root));
    }

    // ==================== Builders ====================

    private static BenchSupport.TreeCase wideNoWrap(int nodeCount) {
        TaffyTree tree = new TaffyTree();
        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.ROW, 100f, 100f);
        TaffyStyle leafStyle = BenchSupport.sized(10f, 10f);
        NodeId[] children = new NodeId[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            children[i] = tree.newLeaf(leafStyle);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, children));
    }

    /**
     * {@code fit_nesting.txt}: 10x10x10x100 nested flex boxes. Level 1 and 3 are rows, level 2
     * is a column, and every level is {@code width: 100%, height: auto} except the fixed-height
     * leaves. The root has 5px padding and a 1000px definite width.
     */
    private static BenchSupport.TreeCase fitNesting() {
        TaffyTree tree = new TaffyTree();

        TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, 1000f, Float.NaN);
        BenchSupport.padding(rootStyle, 5f);

        TaffyStyle rowStyle = BenchSupport.sizedFlex(FlexDirection.ROW, Float.NaN, Float.NaN);
        rowStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        TaffyStyle columnStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, Float.NaN, Float.NaN);
        columnStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.AUTO);
        TaffyStyle leafStyle = new TaffyStyle();
        leafStyle.size = new TaffySize<>(TaffyDimension.percent(1f), TaffyDimension.length(10f));

        NodeId[] level1 = new NodeId[10];
        for (int a = 0; a < 10; a++) {
            NodeId[] level2 = new NodeId[10];
            for (int b = 0; b < 10; b++) {
                NodeId[] level3 = new NodeId[10];
                for (int c = 0; c < 10; c++) {
                    NodeId[] leaves = new NodeId[100];
                    for (int d = 0; d < 100; d++) {
                        leaves[d] = tree.newLeaf(leafStyle);
                    }
                    level3[c] = tree.newWithChildren(rowStyle, leaves);
                }
                level2[b] = tree.newWithChildren(columnStyle, level3);
            }
            level1[a] = tree.newWithChildren(rowStyle, level2);
        }
        return new BenchSupport.TreeCase(tree, tree.newWithChildren(rootStyle, level1));
    }
}
