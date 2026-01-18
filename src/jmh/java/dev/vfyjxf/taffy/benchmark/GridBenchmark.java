package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.Size;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.Dimension;
import dev.vfyjxf.taffy.style.Display;
import dev.vfyjxf.taffy.style.Style;
import dev.vfyjxf.taffy.style.TrackSizingFunction;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Grid layout benchmarks - mirrors Rust taffy benches/grid.rs
 *
 * Test groups (matching Rust):
 * - grid/wide: NxN grid (cells = N²) with random track sizes
 * - grid/deep: Nested NxN grids at various depths
 * - grid/superdeep: Deep 1x1 grid chain
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class GridBenchmark {

    private static final long SEED = 12345L;

    // ==================== grid/wide ====================
    // Rust: NxN grid where total cells = track_count²
    // Use a single benchmark with @Param (matches Rust bench group inputs)

    @State(Scope.Thread)
    public static class WideGridState {
        TaffyTree tree;
        NodeId root;

        // Rust uses track_count [31, 100, 316]
        @Param({"31", "100", "316"})
        int trackCount;

        @Setup(Level.Invocation)
        public void setup() {
            Object[] result = buildGridFlatHierarchy(trackCount, trackCount);
            tree = (TaffyTree) result[0];
            root = (NodeId) result[1];
        }
    }

    @Benchmark
    public void wideGrid(WideGridState state, Blackhole bh) {
        state.tree.computeLayout(state.root, Size.of(AvailableSpace.definite(12000f), AvailableSpace.definite(12000f)));
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== grid/deep ====================
    // Rust: Nested NxN grids, various configurations
    // Configurations: [(2, 5), (3, 4), (2, 7)] = (tracks, levels)

    @State(Scope.Thread)
    public static class DeepGridState {
        TaffyTree tree;
        NodeId root;

        // "tracks_levels" format: "2_5" means 2x2 grid, 5 levels deep
        @Param({"2_5", "3_4", "2_7"})
        String config;

        @Setup(Level.Invocation)
        public void setup() {
            String[] parts = config.split("_");
            int tracks = Integer.parseInt(parts[0]);
            int levels = Integer.parseInt(parts[1]);

            // Rust: build_taffy_deep_grid_hierarchy(levels, tracks)
            Object[] result = buildTaffyDeepGridHierarchy(levels, tracks);
            tree = (TaffyTree) result[0];
            root = (NodeId) result[1];
        }
    }

    @Benchmark
    public void deepGrid(DeepGridState state, Blackhole bh) {
        // Rust uses: taffy.compute_layout(root, length(12000.0))
        state.tree.computeLayout(state.root, Size.of(AvailableSpace.definite(12000f), AvailableSpace.definite(12000f)));
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== grid/superdeep ====================
    // Rust: Very deep 1x1 grid chain
    // Depths: [100, 1000]

    @State(Scope.Thread)
    public static class SuperDeepGridState {
        TaffyTree tree;
        NodeId root;

        @Param({"100", "1000"})
        int depth;

        @Setup(Level.Invocation)
        public void setup() {
            // Rust: build_taffy_deep_grid_hierarchy(levels, 1)
            Object[] result = buildTaffyDeepGridHierarchy(depth, 1);
            tree = (TaffyTree) result[0];
            root = (NodeId) result[1];
        }
    }

    @Benchmark
    public void superDeepGrid(SuperDeepGridState state, Blackhole bh) {
        // Rust uses: taffy.compute_layout(root, max_content())
        state.tree.computeLayout(state.root, Size.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Helper Methods ====================

    /**
     * Build a random leaf node (matching Rust's build_random_leaf)
     * Rust: Style { size: length(20.0), ..Default::default() }
     */
    private static NodeId buildRandomLeaf(TaffyTree taffy, Random rng) {
        Style style = new Style();
        style.size = new Size<>(Dimension.length(20.0f), Dimension.length(20.0f));
        return taffy.newLeaf(style);
    }

    /**
     * Generate random grid track (matching Rust's random_grid_track)
     * 
     * Distribution (from Rust):
     * - 0.0..0.1: auto()
     * - 0.1..0.2: min_content()
     * - 0.2..0.3: max_content()
     * - 0.3..0.5: fr(1.0)
     * - 0.5..0.6: minmax(length(0.0), fr(1.0))
     * - 0.6..0.8: length(40.0)
     * - 0.8..1.0: percent(0.3)
     */
    private static TrackSizingFunction randomGridTrack(Random rng) {
        float r = rng.nextFloat();
        
        if (r < 0.1f) {
            return TrackSizingFunction.auto();
        } else if (r < 0.2f) {
            return TrackSizingFunction.minContent();
        } else if (r < 0.3f) {
            return TrackSizingFunction.maxContent();
        } else if (r < 0.5f) {
            return TrackSizingFunction.fr(1.0f);
        } else if (r < 0.6f) {
            return TrackSizingFunction.minmax(
                TrackSizingFunction.fixed(0.0f),
                TrackSizingFunction.fr(1.0f)
            );
        } else if (r < 0.8f) {
            return TrackSizingFunction.fixed(40.0f);
        } else {
            return TrackSizingFunction.percent(0.3f);
        }
    }

    /**
     * Create random NxN grid style (matching Rust's random_nxn_grid_style)
     */
    private static Style randomNxNGridStyle(Random rng, int trackCount) {
        Style style = new Style();
        style.display = Display.GRID;
        
        List<TrackSizingFunction> cols = new ArrayList<>(trackCount);
        List<TrackSizingFunction> rows = new ArrayList<>(trackCount);
        for (int i = 0; i < trackCount; i++) {
            cols.add(randomGridTrack(rng));
            rows.add(randomGridTrack(rng));
        }
        style.gridTemplateColumns = cols;
        style.gridTemplateRows = rows;
        
        return style;
    }

    /**
     * A tree with many children that have shallow depth
     * (matching Rust's build_grid_flat_hierarchy)
     */
    private static Object[] buildGridFlatHierarchy(int colCount, int rowCount) {
        TaffyTree taffy = new TaffyTree();
        Random rng = new Random(SEED);

        // Create root style with random grid tracks
        Style style = new Style();
        style.display = Display.GRID;
        
        List<TrackSizingFunction> cols = new ArrayList<>(colCount);
        List<TrackSizingFunction> rows = new ArrayList<>(rowCount);
        for (int i = 0; i < colCount; i++) {
            cols.add(randomGridTrack(rng));
        }
        for (int i = 0; i < rowCount; i++) {
            rows.add(randomGridTrack(rng));
        }
        style.gridTemplateColumns = cols;
        style.gridTemplateRows = rows;

        // Create children (colCount * rowCount leaf nodes)
        int childCount = colCount * rowCount;
        NodeId[] children = new NodeId[childCount];
        for (int i = 0; i < childCount; i++) {
            children[i] = buildRandomLeaf(taffy, rng);
        }

        NodeId root = taffy.newWithChildren(style, children);
        return new Object[]{taffy, root};
    }

    /**
     * A helper function to recursively construct a deep tree
     * (matching Rust's build_deep_grid_tree)
     */
    private static List<NodeId> buildDeepGridTree(
            TaffyTree tree,
            int levels,
            int trackCount,
            Random leafRng,
            Random containerRng
    ) {
        int childCount = trackCount * trackCount;

        if (levels == 1) {
            // Build leaf nodes
            List<NodeId> leaves = new ArrayList<>(childCount);
            for (int i = 0; i < childCount; i++) {
                leaves.add(buildRandomLeaf(tree, leafRng));
            }
            return leaves;
        }

        // Add another layer to the tree
        // Each child gets an equal amount of the remaining nodes
        List<NodeId> nodes = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            List<NodeId> subChildren = buildDeepGridTree(tree, levels - 1, trackCount, leafRng, containerRng);
            Style gridStyle = randomNxNGridStyle(containerRng, trackCount);
            NodeId container = tree.newWithChildren(gridStyle, subChildren.toArray(new NodeId[0]));
            nodes.add(container);
        }
        return nodes;
    }

    /**
     * A tree with a higher depth for a more realistic scenario
     * (matching Rust's build_taffy_deep_grid_hierarchy)
     */
    private static Object[] buildTaffyDeepGridHierarchy(int levels, int trackCount) {
        // Rust uses separate RNG instances seeded from the same value
        Random leafRng = new Random(SEED);
        Random containerRng = new Random(SEED);

        TaffyTree taffy = new TaffyTree();

        // Special-case for the superdeep 1x1 grid chain.
        // The recursive builder is logically fine, but for levels=1000 it can overflow the Java stack
        // during tree construction (before we even get to computeLayout).
        // This iterative path preserves the same RNG usage order as the recursive variant for trackCount=1.
        if (trackCount == 1 && levels > 200) {
            NodeId child = buildRandomLeaf(taffy, leafRng);
            for (int lvl = 2; lvl <= levels; lvl++) {
                Style gridStyle = randomNxNGridStyle(containerRng, 1);
                child = taffy.newWithChildren(gridStyle, new NodeId[]{child});
            }

            Style rootStyle = new Style();
            NodeId root = taffy.newWithChildren(rootStyle, new NodeId[]{child});
            return new Object[]{taffy, root};
        }

        List<NodeId> tree = buildDeepGridTree(taffy, levels, trackCount, leafRng, containerRng);
        
        // Root with default style
        Style rootStyle = new Style();
        NodeId root = taffy.newWithChildren(rootStyle, tree.toArray(new NodeId[0]));
        
        return new Object[]{taffy, root};
    }
}
