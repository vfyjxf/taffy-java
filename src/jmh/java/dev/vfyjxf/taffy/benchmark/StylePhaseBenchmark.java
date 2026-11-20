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
 * Style-write and invalidation phases.
 *
 * <p>This port has no style parser: a {@code TaffyStyle} is a plain Java object and
 * {@code TaffyTree.setStyle} only stores the reference, bumps the direction epoch and marks the
 * node's cache dirty (which walks to the root while caches are still non-empty). This class
 * prices exactly that path, separately from layout.</p>
 *
 * <p>Every tree is a flat column flex root with {@code nodeCount - 1} fixed 10x10 leaves
 * ({@code nodeCount = 1k / 10k / 100k}), built and laid out once in
 * {@code @Setup(Level.Trial)}.</p>
 *
 * <p>Scenarios:</p>
 * <ul>
 *   <li>{@code setStyleSameValue}: {@value #STYLE_WRITES} writes of the same style instance to
 *       one leaf, no layout. Normalised with {@code @OperationsPerInvocation} so the score is one
 *       style write;</li>
 *   <li>{@code setStyleDifferentValue}: the same loop alternating two style instances with
 *       different heights;</li>
 *   <li>{@code setStyleAcrossLeaves}: the same number of writes spread over distinct leaves, so
 *       each write pays its own id lookup (and the first pass pays the ancestor walk);</li>
 *   <li>{@code setStyleSameValueThenLayout} / {@code setStyleDifferentValueThenLayout}: the same
 *       write batch followed by one layout, to compare write cost with write + relayout cost;</li>
 *   <li>{@code styleChangeRelayout}: one {@code setStyle} + one layout (the upstream relayout
 *       shape, flat instead of wide/balanced/deep);</li>
 *   <li>{@code computeAfterMarkDirty}: one {@code markDirty} + one layout with no style write, so
 *       the difference against {@code styleChangeRelayout} is the style-write cost itself.</li>
 * </ul>
 *
 * <p>Rounding is disabled: it is measured by {@code PhaseLayoutBenchmark} and would add a
 * tree-wide pass to every layout here.</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class StylePhaseBenchmark {

    /** Number of style writes performed by one measured invocation of the batch methods. */
    static final int STYLE_WRITES = 10_000;

    private static final float LAYOUT_WIDTH = 2000f;

    @State(Scope.Thread)
    public static class StyleState {
        @Param({"1000", "10000", "100000"})
        public int nodeCount;

        TaffyTree tree;
        NodeId root;
        NodeId leaf;
        NodeId[] leaves;
        NodeId[] writeTargets;
        TaffyStyle shortStyle;
        TaffyStyle tallStyle;
        boolean tall;

        @Setup(Level.Trial)
        public void setupTrial() {
            tree = new TaffyTree();
            tree.disableRounding();
            shortStyle = leafStyle(10f);
            tallStyle = leafStyle(20f);
            TaffyStyle rootStyle = BenchSupport.sizedFlex(FlexDirection.COLUMN, LAYOUT_WIDTH, Float.NaN);
            int childCount = Math.max(1, nodeCount - 1);
            leaves = new NodeId[childCount];
            for (int i = 0; i < childCount; i++) {
                // Start from the "short" value so the first same-value write is a no-op change.
                leaves[i] = tree.newLeaf(shortStyle);
            }
            root = tree.newWithChildren(rootStyle, leaves);
            leaf = leaves[0];
            // Pre-compute the distinct-node write target list so the measured loop contains only
            // the setStyle call and the alternating style choice.
            writeTargets = new NodeId[STYLE_WRITES];
            for (int i = 0; i < STYLE_WRITES; i++) {
                writeTargets[i] = leaves[i % leaves.length];
            }
            tree.computeLayout(root, layoutSpace());
        }
    }

    @Benchmark
    @OperationsPerInvocation(STYLE_WRITES)
    public void setStyleSameValue(StyleState state, Blackhole bh) {
        TaffyStyle style = state.shortStyle;
        for (int i = 0; i < STYLE_WRITES; i++) {
            state.tree.setStyle(state.leaf, style);
        }
        bh.consume(state.tree);
    }

    @Benchmark
    @OperationsPerInvocation(STYLE_WRITES)
    public void setStyleDifferentValue(StyleState state, Blackhole bh) {
        TaffyStyle shortStyle = state.shortStyle;
        TaffyStyle tallStyle = state.tallStyle;
        for (int i = 0; i < STYLE_WRITES; i++) {
            state.tree.setStyle(state.leaf, (i & 1) == 0 ? shortStyle : tallStyle);
        }
        bh.consume(state.tree);
    }

    @Benchmark
    @OperationsPerInvocation(STYLE_WRITES)
    public void setStyleAcrossLeaves(StyleState state, Blackhole bh) {
        NodeId[] targets = state.writeTargets;
        TaffyStyle shortStyle = state.shortStyle;
        TaffyStyle tallStyle = state.tallStyle;
        for (int i = 0; i < STYLE_WRITES; i++) {
            state.tree.setStyle(targets[i], (i & 1) == 0 ? shortStyle : tallStyle);
        }
        bh.consume(state.tree);
    }

    @Benchmark
    public void setStyleSameValueThenLayout(StyleState state, Blackhole bh) {
        TaffyStyle style = state.shortStyle;
        for (int i = 0; i < STYLE_WRITES; i++) {
            state.tree.setStyle(state.leaf, style);
        }
        state.tree.computeLayout(state.root, layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void setStyleDifferentValueThenLayout(StyleState state, Blackhole bh) {
        TaffyStyle shortStyle = state.shortStyle;
        TaffyStyle tallStyle = state.tallStyle;
        for (int i = 0; i < STYLE_WRITES; i++) {
            state.tree.setStyle(state.leaf, (i & 1) == 0 ? shortStyle : tallStyle);
        }
        state.tree.computeLayout(state.root, layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void styleChangeRelayout(StyleState state, Blackhole bh) {
        state.tall = !state.tall;
        state.tree.setStyle(state.leaf, state.tall ? state.tallStyle : state.shortStyle);
        state.tree.computeLayout(state.root, layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    @Benchmark
    public void computeAfterMarkDirty(StyleState state, Blackhole bh) {
        state.tree.markDirty(state.leaf);
        state.tree.computeLayout(state.root, layoutSpace());
        bh.consume(state.tree.getLayout(state.root));
    }

    // === Helpers ===

    private static TaffySize<AvailableSpace> layoutSpace() {
        return TaffySize.of(AvailableSpace.definite(LAYOUT_WIDTH), AvailableSpace.maxContent());
    }

    private static TaffyStyle leafStyle(float height) {
        TaffyStyle style = new TaffyStyle();
        style.size = new TaffySize<>(TaffyDimension.length(10f), TaffyDimension.length(height));
        return style;
    }
}
