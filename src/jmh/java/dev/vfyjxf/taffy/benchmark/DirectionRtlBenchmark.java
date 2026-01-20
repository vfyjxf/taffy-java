package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.*;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks for RTL direction and JustifyContent.STRETCH functionality.
 * Tests the performance impact of Direction.RTL and Grid STRETCH behavior.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class DirectionRtlBenchmark {

    private static final long SEED = 12345L;

    // ==================== Flexbox LTR vs RTL Comparison ====================

    @State(Scope.Thread)
    public static class FlexboxDirectionState {
        TaffyTree treeLtr;
        TaffyTree treeRtl;
        NodeId rootLtr;
        NodeId rootRtl;

        @Param({"100", "500", "1000"})
        int childCount;

        @Setup(Level.Invocation)
        public void setup() {
            // LTR tree
            treeLtr = new TaffyTree();
            TaffyStyle containerLtr = new TaffyStyle();
            containerLtr.display = TaffyDisplay.FLEX;
            containerLtr.flexDirection = FlexDirection.ROW;
            containerLtr.flexWrap = FlexWrap.WRAP;
            containerLtr.direction = TaffyDirection.LTR;
            containerLtr.size = new TaffySize<>(TaffyDimension.length(1000f), TaffyDimension.auto());
            rootLtr = treeLtr.newLeaf(containerLtr);

            // RTL tree
            treeRtl = new TaffyTree();
            TaffyStyle containerRtl = new TaffyStyle();
            containerRtl.display = TaffyDisplay.FLEX;
            containerRtl.flexDirection = FlexDirection.ROW;
            containerRtl.flexWrap = FlexWrap.WRAP;
            containerRtl.direction = TaffyDirection.RTL;
            containerRtl.size = new TaffySize<>(TaffyDimension.length(1000f), TaffyDimension.auto());
            rootRtl = treeRtl.newLeaf(containerRtl);

            Random rng = new Random(SEED);
            for (int i = 0; i < childCount; i++) {
                float width = 50 + rng.nextFloat() * 100;
                float height = 30 + rng.nextFloat() * 50;

                TaffyStyle childStyle = new TaffyStyle();
                childStyle.size = new TaffySize<>(TaffyDimension.length(width), TaffyDimension.length(height));
                childStyle.margin = TaffyRect.all(LengthPercentageAuto.length(5f));

                treeLtr.addChild(rootLtr, treeLtr.newLeaf(childStyle));
                treeRtl.addChild(rootRtl, treeRtl.newLeaf(childStyle));
            }
        }
    }

    @Benchmark
    public void flexboxLtr(FlexboxDirectionState state, Blackhole bh) {
        state.treeLtr.computeLayout(state.rootLtr, TaffySize.maxContent());
        bh.consume(state.treeLtr.getLayout(state.rootLtr));
    }

    @Benchmark
    public void flexboxRtl(FlexboxDirectionState state, Blackhole bh) {
        state.treeRtl.computeLayout(state.rootRtl, TaffySize.maxContent());
        bh.consume(state.treeRtl.getLayout(state.rootRtl));
    }

    // ==================== Flexbox Row-Reverse with RTL ====================

    @State(Scope.Thread)
    public static class FlexboxRowReverseRtlState {
        TaffyTree tree;
        NodeId root;

        @Param({"100", "500"})
        int childCount;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            TaffyStyle containerStyle = new TaffyStyle();
            containerStyle.display = TaffyDisplay.FLEX;
            containerStyle.flexDirection = FlexDirection.ROW_REVERSE;
            containerStyle.direction = TaffyDirection.RTL;
            containerStyle.size = new TaffySize<>(TaffyDimension.length(1000f), TaffyDimension.auto());
            root = tree.newLeaf(containerStyle);

            Random rng = new Random(SEED);
            for (int i = 0; i < childCount; i++) {
                TaffyStyle childStyle = new TaffyStyle();
                childStyle.size = new TaffySize<>(
                    TaffyDimension.length(50 + rng.nextFloat() * 50),
                    TaffyDimension.length(30 + rng.nextFloat() * 30)
                );
                tree.addChild(root, tree.newLeaf(childStyle));
            }
        }
    }

    @Benchmark
    public void flexboxRowReverseRtl(FlexboxRowReverseRtlState state, Blackhole bh) {
        state.tree.computeLayout(state.root, TaffySize.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }

    // ==================== Grid with JustifyContent.STRETCH ====================

    @State(Scope.Thread)
    public static class GridStretchState {
        TaffyTree treeStretch;
        TaffyTree treeStart;
        NodeId rootStretch;
        NodeId rootStart;

        @Param({"10", "20", "50"})
        int trackCount;

        @Setup(Level.Invocation)
        public void setup() {
            // Grid with STRETCH (default)
            treeStretch = new TaffyTree();
            TaffyStyle containerStretch = new TaffyStyle();
            containerStretch.display = TaffyDisplay.GRID;
            containerStretch.size = new TaffySize<>(TaffyDimension.length(1000f), TaffyDimension.length(1000f));
            containerStretch.gridTemplateColumns = createAutoTracks(trackCount);
            containerStretch.gridTemplateRows = createAutoTracks(trackCount);
            // justifyContent = null means STRETCH (default for Grid)
            rootStretch = treeStretch.newLeaf(containerStretch);

            // Grid with START (no stretch)
            treeStart = new TaffyTree();
            TaffyStyle containerStart = new TaffyStyle();
            containerStart.display = TaffyDisplay.GRID;
            containerStart.size = new TaffySize<>(TaffyDimension.length(1000f), TaffyDimension.length(1000f));
            containerStart.gridTemplateColumns = createAutoTracks(trackCount);
            containerStart.gridTemplateRows = createAutoTracks(trackCount);
            containerStart.justifyContent = AlignContent.START;
            rootStart = treeStart.newLeaf(containerStart);

            // Add children
            Random rng = new Random(SEED);
            int cellCount = trackCount * trackCount;
            for (int i = 0; i < cellCount; i++) {
                TaffyStyle childStyle = new TaffyStyle();
                childStyle.size = new TaffySize<>(
                    TaffyDimension.length(10 + rng.nextFloat() * 20),
                    TaffyDimension.length(10 + rng.nextFloat() * 20)
                );

                treeStretch.addChild(rootStretch, treeStretch.newLeaf(childStyle));
                treeStart.addChild(rootStart, treeStart.newLeaf(childStyle));
            }
        }

        private List<TrackSizingFunction> createAutoTracks(int count) {
            java.util.ArrayList<TrackSizingFunction> tracks = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                tracks.add(TrackSizingFunction.auto());
            }
            return tracks;
        }
    }

    @Benchmark
    public void gridJustifyStretch(GridStretchState state, Blackhole bh) {
        state.treeStretch.computeLayout(state.rootStretch,
            TaffySize.of(AvailableSpace.definite(1000f), AvailableSpace.definite(1000f)));
        bh.consume(state.treeStretch.getLayout(state.rootStretch));
    }

    @Benchmark
    public void gridJustifyStart(GridStretchState state, Blackhole bh) {
        state.treeStart.computeLayout(state.rootStart,
            TaffySize.of(AvailableSpace.definite(1000f), AvailableSpace.definite(1000f)));
        bh.consume(state.treeStart.getLayout(state.rootStart));
    }

    // ==================== Grid RTL Column Layout ====================

    @State(Scope.Thread)
    public static class GridRtlState {
        TaffyTree treeLtr;
        TaffyTree treeRtl;
        NodeId rootLtr;
        NodeId rootRtl;

        @Param({"5", "10", "20"})
        int trackCount;

        @Setup(Level.Invocation)
        public void setup() {
            int cellCount = trackCount * trackCount;

            // LTR Grid
            treeLtr = new TaffyTree();
            TaffyStyle containerLtr = new TaffyStyle();
            containerLtr.display = TaffyDisplay.GRID;
            containerLtr.direction = TaffyDirection.LTR;
            containerLtr.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(800f));
            containerLtr.gridTemplateColumns = createFixedTracks(trackCount, 800f / trackCount);
            containerLtr.gridTemplateRows = createFixedTracks(trackCount, 800f / trackCount);
            rootLtr = treeLtr.newLeaf(containerLtr);

            // RTL Grid
            treeRtl = new TaffyTree();
            TaffyStyle containerRtl = new TaffyStyle();
            containerRtl.display = TaffyDisplay.GRID;
            containerRtl.direction = TaffyDirection.RTL;
            containerRtl.size = new TaffySize<>(TaffyDimension.length(800f), TaffyDimension.length(800f));
            containerRtl.gridTemplateColumns = createFixedTracks(trackCount, 800f / trackCount);
            containerRtl.gridTemplateRows = createFixedTracks(trackCount, 800f / trackCount);
            rootRtl = treeRtl.newLeaf(containerRtl);

            for (int i = 0; i < cellCount; i++) {
                TaffyStyle childStyle = new TaffyStyle();
                childStyle.size = new TaffySize<>(TaffyDimension.percent(100f), TaffyDimension.percent(100f));
                
                treeLtr.addChild(rootLtr, treeLtr.newLeaf(childStyle));
                treeRtl.addChild(rootRtl, treeRtl.newLeaf(childStyle));
            }
        }

        private List<TrackSizingFunction> createFixedTracks(int count, float size) {
            java.util.ArrayList<TrackSizingFunction> tracks = new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                tracks.add(TrackSizingFunction.fixed(LengthPercentage.length(size)));
            }
            return tracks;
        }
    }

    @Benchmark
    public void gridLtr(GridRtlState state, Blackhole bh) {
        state.treeLtr.computeLayout(state.rootLtr,
            TaffySize.of(AvailableSpace.definite(800f), AvailableSpace.definite(800f)));
        bh.consume(state.treeLtr.getLayout(state.rootLtr));
    }

    @Benchmark
    public void gridRtl(GridRtlState state, Blackhole bh) {
        state.treeRtl.computeLayout(state.rootRtl,
            TaffySize.of(AvailableSpace.definite(800f), AvailableSpace.definite(800f)));
        bh.consume(state.treeRtl.getLayout(state.rootRtl));
    }

    // ==================== Direction Inheritance Benchmark ====================

    @State(Scope.Thread)
    public static class DirectionInheritState {
        TaffyTree tree;
        NodeId root;

        @Param({"3", "5", "7"})
        int depth;

        @Setup(Level.Invocation)
        public void setup() {
            tree = new TaffyTree();
            
            // Root with explicit RTL direction
            TaffyStyle rootStyle = new TaffyStyle();
            rootStyle.display = TaffyDisplay.FLEX;
            rootStyle.flexDirection = FlexDirection.COLUMN;
            rootStyle.direction = TaffyDirection.RTL;
            rootStyle.size = new TaffySize<>(TaffyDimension.length(1000f), TaffyDimension.auto());
            root = tree.newLeaf(rootStyle);

            // Build nested structure with INHERIT direction
            buildNestedInherit(tree, root, depth, 3);
        }

        private void buildNestedInherit(TaffyTree tree, NodeId parent, int remainingDepth, int childrenPerLevel) {
            if (remainingDepth <= 0) return;

            for (int i = 0; i < childrenPerLevel; i++) {
                TaffyStyle childStyle = new TaffyStyle();
                childStyle.display = TaffyDisplay.FLEX;
                childStyle.flexDirection = (remainingDepth % 2 == 0) ? FlexDirection.ROW : FlexDirection.COLUMN;
                childStyle.direction = TaffyDirection.INHERIT; // Should inherit RTL from root
                childStyle.size = new TaffySize<>(TaffyDimension.percent(100f), TaffyDimension.auto());
                childStyle.padding = TaffyRect.all(LengthPercentage.length(5f));

                NodeId child = tree.newLeaf(childStyle);
                tree.addChild(parent, child);

                if (remainingDepth > 1) {
                    // Add some leaf children
                    for (int j = 0; j < 2; j++) {
                        TaffyStyle leafStyle = new TaffyStyle();
                        leafStyle.size = new TaffySize<>(TaffyDimension.length(50f), TaffyDimension.length(30f));
                        tree.addChild(child, tree.newLeaf(leafStyle));
                    }
                    buildNestedInherit(tree, child, remainingDepth - 1, childrenPerLevel);
                }
            }
        }
    }

    @Benchmark
    public void directionInheritance(DirectionInheritState state, Blackhole bh) {
        state.tree.computeLayout(state.root, TaffySize.maxContent());
        bh.consume(state.tree.getLayout(state.root));
    }
}
