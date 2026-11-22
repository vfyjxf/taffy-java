package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Internal-phase micro-benchmarks.
 *
 * <p>This class lives in {@code dev.vfyjxf.taffy.tree} on purpose: it measures internals that are
 * either public but meaningless without the package ({@link LayoutCache}) or package-private
 * (the direction epoch bump). It gives each phase a number without any algorithm work on top.</p>
 *
 * <p>Scenarios:</p>
 * <ul>
 *   <li>{@code cacheGetMeasureHitX9}: look up the nine measure entries of a full cache once each
 *       (upstream {@code relayout.rs} {@code cache/get_measure_hit_x9});</li>
 *   <li>{@code cacheGetMeasureMiss}: a lookup that compares against all nine measure entries and
 *       misses (upstream {@code cache/get_measure_miss});</li>
 *   <li>{@code cacheGetLayoutHit}: one PERFORM_LAYOUT hit (upstream {@code cache/get_layout_hit});</li>
 *   <li>{@code cacheStoreX10}: fill an empty cache with one final-layout entry and nine measure
 *       entries (upstream {@code cache/store_x10});</li>
 *   <li>{@code resolveDirectionWarm}: repeated direction queries on a node whose memo stamp is
 *       current (one id lookup plus a field read);</li>
 *   <li>{@code resolveDirectionCold}: bump the tree's direction epoch, then resolve the deepest
 *       leaf of a {@code depth}-level INHERIT chain, so the walk and the memo writes are paid
 *       again;</li>
 *   <li>{@code roundLayout}: the tree-wide rounding pass on an already laid-out flat tree,
 *       {@code nodeCount = 1k / 10k / 100k}.</li>
 * </ul>
 *
 * <p>All cache keys are constructed once in trial setup. {@code LayoutCache} stores the key
 * fields in place, so the measured calls do not allocate except where noted.</p>
 *
 * <p>Rounding is disabled in the layout that precedes {@code roundLayout}; the measured call is
 * then exactly one rounding pass. The direction benchmarks use the same {@link LayoutComputer}
 * the layout pass uses, but never lay the tree out, so no other phase contributes.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class InternalPhaseBenchmark {

    private static final int MEASURE_ENTRIES = 9;
    private static final int WARM_DIRECTION_CALLS = 1000;

    // === LayoutCache micro-benchmarks ===

    @State(Scope.Thread)
    public static class CacheState {
        LayoutCache cache;
        LayoutInput layoutInput;
        LayoutInput[] measureInputs;
        LayoutInput missInput;
        LayoutOutput layoutOutput;

        @Setup(Level.Trial)
        public void setupTrial() {
            cache = new LayoutCache();
            layoutInput = layoutInput(0f);
            layoutOutput = LayoutOutput.fromOuterSize(new FloatSize(0f, 0f));
            cache.store(layoutInput, layoutOutput);

            measureInputs = new LayoutInput[MEASURE_ENTRIES];
            for (int width = 0; width < MEASURE_ENTRIES; width++) {
                measureInputs[width] = measureInput(width);
                cache.store(measureInputs[width], LayoutOutput.fromOuterSize(new FloatSize(width, width)));
            }
            // A width outside the nine stored entries, so the lookup compares all of them and
            // misses.
            missInput = measureInput(100f);
        }
    }

    @Benchmark
    @OperationsPerInvocation(MEASURE_ENTRIES)
    public void cacheGetMeasureHitX9(CacheState state, Blackhole bh) {
        LayoutCache cache = state.cache;
        for (LayoutInput input : state.measureInputs) {
            bh.consume(cache.get(input));
        }
    }

    @Benchmark
    public void cacheGetMeasureMiss(CacheState state, Blackhole bh) {
        bh.consume(state.cache.get(state.missInput));
    }

    @Benchmark
    public void cacheGetLayoutHit(CacheState state, Blackhole bh) {
        bh.consume(state.cache.get(state.layoutInput));
    }

    @Benchmark
    public void cacheStoreX10(CacheState state, Blackhole bh) {
        LayoutCache cache = new LayoutCache();
        cache.store(state.layoutInput, state.layoutOutput);
        for (LayoutInput input : state.measureInputs) {
            cache.store(input, LayoutOutput.fromOuterSize(new FloatSize(1f, 1f)));
        }
        bh.consume(cache);
    }

    // === Direction resolution ===

    @State(Scope.Thread)
    public static class DirectionState {
        @Param({"64", "256", "1024"})
        public int depth;

        TaffyTree tree;
        NodeId root;
        NodeId leaf;
        LayoutComputer computer;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            TaffyStyle inherit = new TaffyStyle(); // direction = INHERIT
            leaf = tree.newLeaf(inherit);
            NodeId continuing = leaf;
            for (int level = 1; level < depth; level++) {
                continuing = tree.newWithChildren(inherit, continuing);
            }
            TaffyStyle rootStyle = new TaffyStyle();
            rootStyle.direction = TaffyDirection.LTR;
            root = tree.newWithChildren(rootStyle, continuing);
            computer = new LayoutComputer(tree, null);
            // Warm the memo once: the warm benchmark then measures the hit path, and the cold
            // benchmark starts from a fully stamped chain of the same shape.
            computer.resolveDirection(leaf);
        }
    }

    @Benchmark
    @OperationsPerInvocation(WARM_DIRECTION_CALLS)
    public void resolveDirectionWarm(DirectionState state, Blackhole bh) {
        LayoutComputer computer = state.computer;
        NodeId leaf = state.leaf;
        for (int i = 0; i < WARM_DIRECTION_CALLS; i++) {
            bh.consume(computer.resolveDirection(leaf));
        }
    }

    @Benchmark
    public void resolveDirectionCold(DirectionState state, Blackhole bh) {
        state.tree.invalidateResolvedDirections();
        bh.consume(state.computer.resolveDirection(state.leaf));
    }

    // === Rounding pass ===

    @State(Scope.Thread)
    public static class RoundState {
        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            TaffyStyle rootStyle = new TaffyStyle();
            rootStyle.display = dev.vfyjxf.taffy.style.TaffyDisplay.FLEX;
            rootStyle.flexDirection = FlexDirection.COLUMN;
            TaffyStyle leafStyle = new TaffyStyle();
            leafStyle.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(10f));
            int childCount = Math.max(1, nodeCount - 1);
            NodeId[] children = new NodeId[childCount];
            for (int i = 0; i < childCount; i++) {
                children[i] = tree.newLeaf(leafStyle);
            }
            root = tree.newWithChildren(rootStyle, children);
            // Lay out once with rounding disabled so the measured call is exactly one rounding
            // pass; enable it afterwards so getLayout returns the rounded output the pass wrote.
            tree.computeLayout(root, TaffySize.of(
                AvailableSpace.definite(2000f), AvailableSpace.maxContent()));
            tree.enableRounding();
        }
    }

    @Benchmark
    public void roundLayout(RoundState state, Blackhole bh) {
        state.tree.roundLayout(state.root);
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Cache key helpers ===

    private static LayoutInput measureInput(float width) {
        return new LayoutInput(
            RunMode.COMPUTE_SIZE,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(width, Float.NaN),
            new FloatSize(1000f, Float.NaN),
            new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT),
            TaffyLine.FALSE);
    }

    private static LayoutInput layoutInput(float width) {
        return new LayoutInput(
            RunMode.PERFORM_LAYOUT,
            SizingMode.INHERENT_SIZE,
            RequestedAxis.BOTH,
            new FloatSize(width, Float.NaN),
            new FloatSize(1000f, Float.NaN),
            new TaffySize<>(AvailableSpace.MAX_CONTENT, AvailableSpace.MAX_CONTENT),
            TaffyLine.FALSE);
    }
}
