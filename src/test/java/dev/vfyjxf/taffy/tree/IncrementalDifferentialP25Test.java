package dev.vfyjxf.taffy.tree;

import dev.vfyjxf.taffy.geometry.FloatRect;
import dev.vfyjxf.taffy.geometry.FloatSize;
import dev.vfyjxf.taffy.geometry.TaffyRect;
import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.LengthPercentage;
import dev.vfyjxf.taffy.style.LengthPercentageAuto;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.util.MeasureFunc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P25c: seeded differential fuzz of the incremental layout path against a cold tree.
 *
 * <p>The test keeps one {@link TaffyTree} alive across a sequence of random style and child-list
 * mutations and, after every mutation, lays it out with the caches and dirty flags accumulated by
 * the previous steps. It then builds a completely fresh {@link TaffyTree} from the same
 * {@link NodeSpec} tree, lays that one out with the same available space, and compares every node
 * field by field - both the final (rounded) layout and the unrounded layout. A stale cache, a
 * missed invalidation, a wrong dirty propagation or a lost mutation therefore surfaces as a
 * layout mismatch between the two trees.</p>
 *
 * <p>The workload is deliberately small and deterministic: 20 seeds, a depth-3 tree with 2-4
 * children per level, 10 mutation steps per seed, {@code display: flex}/{@code block} only,
 * widths/heights drawn from {10px, 50%, auto}, padding/margin 0-4, gap 0-3 and a 10% chance of
 * {@code position: absolute} per non-root node. Half of the seeds run with rounding enabled and
 * half with rounding disabled. Every step also exercises the listener-driven path: a
 * {@link LayoutChangeListener} is installed, the root cache entry is cleared, layout is computed
 * again and the result is compared once more.</p>
 */
@DisplayName("P25c: incremental layout differential fuzz")
class IncrementalDifferentialP25Test {

    /** Seeds per rounding mode; the test runs 2 * this many seeds in total. */
    private static final int SEEDS_PER_ROUNDING = 10;

    /** Mutations (and incrementally computed layouts) per seed. */
    private static final int STEPS_PER_SEED = 10;

    /** Tree depth of the generated workload (root is depth 0). */
    private static final int MAX_DEPTH = 3;

    /** Available-space values toggled between steps. */
    private static final float[] AVAILABLE_SPACES = {800f, 1000f};

    /** Upper bound on collected mismatch messages so a broken tree cannot flood the report. */
    private static final int MAX_FAILURES = 40;

    @Test
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    @DisplayName("incremental layout equals a freshly built tree after every mutation")
    void incrementalLayoutMatchesFreshTree() {
        long startedAt = System.nanoTime();
        List<String> failures = new ArrayList<>();
        Stats stats = new Stats();

        for (int seed = 1; seed <= SEEDS_PER_ROUNDING * 2; seed++) {
            boolean rounding = seed <= SEEDS_PER_ROUNDING;
            runSeed(seed, rounding, failures, stats);
        }

        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;
        System.out.printf(
            "[P25c] seeds=%d (rounding on=%d, off=%d) steps=%d nodeComparisons=%d "
                + "listenerCalls=%d elapsedMs=%d mismatches=%d%n",
            SEEDS_PER_ROUNDING * 2, SEEDS_PER_ROUNDING, SEEDS_PER_ROUNDING,
            stats.steps, stats.nodesCompared, stats.listenerCalls, elapsedMillis, failures.size());

        assertTrue(failures.isEmpty(), () -> "incremental layout diverged from a fresh tree (first "
            + MAX_FAILURES + " mismatches):\n" + String.join("\n", failures));
    }

    // === Seeded workload ===

    private static void runSeed(int seed, boolean rounding, List<String> failures, Stats stats) {
        Random random = new Random(seed);
        int[] nameCounter = {0};
        NodeSpec rootSpec = generate(random, 0, nameCounter);

        TaffyTree tree = new TaffyTree();
        if (!rounding) {
            tree.disableRounding();
        }
        Map<NodeSpec, NodeId> ids = new IdentityHashMap<>();
        NodeId root = build(tree, rootSpec, ids);

        for (int step = 1; step <= STEPS_PER_SEED; step++) {
            mutate(random, rootSpec, tree, ids, nameCounter);

            float space = AVAILABLE_SPACES[(step - 1) % AVAILABLE_SPACES.length];
            TaffySize<AvailableSpace> availableSpace = TaffySize.of(
                AvailableSpace.definite(space), AvailableSpace.definite(space));

            tree.computeLayout(root, availableSpace);
            stats.steps++;

            TaffyTree fresh = new TaffyTree();
            if (!rounding) {
                fresh.disableRounding();
            }
            Map<NodeSpec, NodeId> freshIds = new IdentityHashMap<>();
            NodeId freshRoot = build(fresh, rootSpec, freshIds);
            fresh.computeLayout(freshRoot, availableSpace);

            String where = "seed=" + seed + " step=" + step;
            int expectedNodeCount = countNodes(rootSpec);
            if (tree.totalNodeCount() != expectedNodeCount) {
                addFailure(failures, where + " totalNodeCount: model=" + expectedNodeCount
                    + " incremental=" + tree.totalNodeCount());
            }
            if (fresh.totalNodeCount() != expectedNodeCount) {
                addFailure(failures, where + " fresh totalNodeCount: model=" + expectedNodeCount
                    + " fresh=" + fresh.totalNodeCount());
            }

            compareTree(seed, step, "", rootSpec, tree, ids, fresh, freshIds, failures, stats);

            // Listener + clearCache variant: installing a listener also disables the tree-clean
            // fast path, so this second pass always walks the layout machinery again.
            int[] calls = {0};
            tree.setLayoutChangeListener((node, oldLayout, newLayout) -> calls[0]++);
            tree.clearCache(root);
            tree.computeLayout(root, availableSpace);
            stats.listenerCalls += calls[0];
            compareTree(seed, step, "[listener+clearCache]", rootSpec, tree, ids, fresh, freshIds,
                failures, stats);
            tree.setLayoutChangeListener(null);
        }
    }

    private static NodeSpec generate(Random random, int depth, int[] nameCounter) {
        NodeSpec spec = new NodeSpec("n" + nameCounter[0]++, depth, randomStyle(random, depth));
        if (depth < MAX_DEPTH) {
            int childCount = 2 + random.nextInt(3);
            for (int i = 0; i < childCount; i++) {
                spec.children.add(generate(random, depth + 1, nameCounter));
            }
        } else if (random.nextInt(10) < 4) {
            spec.measure = glyphMeasure(3 + random.nextInt(8));
        }
        return spec;
    }

    private static TaffyStyle randomStyle(Random random, int depth) {
        TaffyStyle style = new TaffyStyle();
        style.display = random.nextBoolean() ? TaffyDisplay.FLEX : TaffyDisplay.BLOCK;
        style.size = TaffySize.of(randomDimension(random), randomDimension(random));
        style.padding = TaffyRect.of(
            randomPadding(random), randomPadding(random), randomPadding(random), randomPadding(random));
        style.margin = TaffyRect.of(
            randomMargin(random), randomMargin(random), randomMargin(random), randomMargin(random));
        float gap = random.nextInt(4);
        style.gap = TaffySize.of(LengthPercentage.length(gap), LengthPercentage.length(gap));
        if (depth > 0 && random.nextInt(10) == 0) {
            style.position = TaffyPosition.ABSOLUTE;
        }
        return style;
    }

    /** Width/height drawn from fixed 10px, 50% or auto. */
    private static TaffyDimension randomDimension(Random random) {
        int pick = random.nextInt(3);
        if (pick == 0) {
            return TaffyDimension.length(10f);
        }
        if (pick == 1) {
            return TaffyDimension.percent(0.5f);
        }
        return TaffyDimension.AUTO;
    }

    private static LengthPercentage randomPadding(Random random) {
        return LengthPercentage.length(random.nextInt(5));
    }

    private static LengthPercentageAuto randomMargin(Random random) {
        return LengthPercentageAuto.length(random.nextInt(5));
    }

    /** A deterministic text measure: 7px per glyph, one 12px line, clamped by the width space. */
    private static MeasureFunc glyphMeasure(int chars) {
        return (knownDimensions, availableSpace) -> {
            float knownWidth = knownDimensions == null ? Float.NaN : knownDimensions.width;
            float knownHeight = knownDimensions == null ? Float.NaN : knownDimensions.height;
            if (!Float.isNaN(knownWidth) && !Float.isNaN(knownHeight)) {
                return new FloatSize(knownWidth, knownHeight);
            }
            float naturalWidth = chars * 7f;
            float height = 12f;
            float width = naturalWidth;
            if (!Float.isNaN(knownWidth)) {
                width = knownWidth;
            } else if (availableSpace != null && availableSpace.width != null) {
                AvailableSpace widthSpace = availableSpace.width;
                if (widthSpace.isDefinite()) {
                    width = Math.min(naturalWidth, widthSpace.getValue());
                } else if (widthSpace.isMinContent()) {
                    width = 0f;
                }
            }
            if (!Float.isNaN(knownHeight)) {
                height = knownHeight;
            }
            return new FloatSize(width, height);
        };
    }

    // === Build and mutate ===

    private static NodeId build(TaffyTree tree, NodeSpec spec, Map<NodeSpec, NodeId> ids) {
        NodeId id;
        if (spec.children.isEmpty()) {
            id = buildLeaf(tree, spec);
        } else {
            List<NodeId> childIds = new ArrayList<>(spec.children.size());
            for (NodeSpec child : spec.children) {
                childIds.add(build(tree, child, ids));
            }
            id = tree.newWithChildren(spec.style, childIds);
        }
        ids.put(spec, id);
        return id;
    }

    private static NodeId buildLeaf(TaffyTree tree, NodeSpec spec) {
        return spec.measure != null
            ? tree.newLeafWithMeasure(spec.style, spec.measure)
            : tree.newLeaf(spec.style);
    }

    private static void mutate(Random random, NodeSpec root, TaffyTree tree,
                               Map<NodeSpec, NodeId> ids, int[] nameCounter) {
        List<NodeSpec> all = new ArrayList<>();
        collect(root, all);

        boolean mutated = false;
        switch (random.nextInt(3)) {
            case 0 -> mutated = changeWidth(random, all, tree, ids);
            case 1 -> mutated = addLeaf(random, all, tree, ids, nameCounter);
            default -> mutated = removeLeaf(random, all, tree, ids);
        }
        if (!mutated) {
            changeWidth(random, all, tree, ids);
        }
    }

    /** Toggles one node's width between 10px and 20px, keeping the style model in sync. */
    private static boolean changeWidth(Random random, List<NodeSpec> all, TaffyTree tree,
                                       Map<NodeSpec, NodeId> ids) {
        if (all.isEmpty()) {
            return false;
        }
        NodeSpec spec = all.get(random.nextInt(all.size()));
        float nextWidth = 10f;
        TaffyDimension current = spec.style.size.width;
        if (current != null && current.isLength() && Math.abs(current.getValue() - 10f) < 0.0001f) {
            nextWidth = 20f;
        }
        spec.style.size = TaffySize.of(TaffyDimension.length(nextWidth), spec.style.size.height);
        tree.setStyle(ids.get(spec), spec.style);
        return true;
    }

    /** Appends a new leaf under a container node, keeping the model tree in sync. */
    private static boolean addLeaf(Random random, List<NodeSpec> all, TaffyTree tree,
                                   Map<NodeSpec, NodeId> ids, int[] nameCounter) {
        List<NodeSpec> parents = new ArrayList<>();
        for (NodeSpec spec : all) {
            if (spec.depth < MAX_DEPTH && !spec.children.isEmpty()) {
                parents.add(spec);
            }
        }
        if (parents.isEmpty()) {
            return false;
        }
        NodeSpec parent = parents.get(random.nextInt(parents.size()));
        NodeSpec leaf = new NodeSpec("n" + nameCounter[0]++, parent.depth + 1, randomStyle(random, parent.depth + 1));
        if (random.nextInt(2) == 0) {
            leaf.measure = glyphMeasure(3 + random.nextInt(8));
        }
        NodeId childId = buildLeaf(tree, leaf);
        tree.addChild(ids.get(parent), childId);
        parent.children.add(leaf);
        ids.put(leaf, childId);
        return true;
    }

    /** Removes one leaf child from a container node with at least two children. */
    private static boolean removeLeaf(Random random, List<NodeSpec> all, TaffyTree tree,
                                      Map<NodeSpec, NodeId> ids) {
        List<NodeSpec> parents = new ArrayList<>();
        for (NodeSpec spec : all) {
            if (spec.children.size() < 2) {
                continue;
            }
            for (NodeSpec child : spec.children) {
                if (child.children.isEmpty()) {
                    parents.add(spec);
                    break;
                }
            }
        }
        if (parents.isEmpty()) {
            return false;
        }
        NodeSpec parent = parents.get(random.nextInt(parents.size()));
        List<NodeSpec> leaves = new ArrayList<>();
        for (NodeSpec child : parent.children) {
            if (child.children.isEmpty()) {
                leaves.add(child);
            }
        }
        NodeSpec victim = leaves.get(random.nextInt(leaves.size()));
        parent.children.remove(victim);
        NodeId victimId = ids.remove(victim);
        tree.remove(victimId);
        return true;
    }

    private static void collect(NodeSpec spec, List<NodeSpec> out) {
        out.add(spec);
        for (NodeSpec child : spec.children) {
            collect(child, out);
        }
    }

    private static int countNodes(NodeSpec spec) {
        int count = 1;
        for (NodeSpec child : spec.children) {
            count += countNodes(child);
        }
        return count;
    }

    // === Differential comparison ===

    private static void compareTree(int seed, int step, String variant, NodeSpec spec,
                                    TaffyTree incrementalTree, Map<NodeSpec, NodeId> incrementalIds,
                                    TaffyTree freshTree, Map<NodeSpec, NodeId> freshIds,
                                    List<String> failures, Stats stats) {
        compareNode(seed, step, variant, "", spec, incrementalTree, incrementalIds,
            freshTree, freshIds, failures, stats);
    }

    private static void compareNode(int seed, int step, String variant, String parentPath, NodeSpec spec,
                                    TaffyTree incrementalTree, Map<NodeSpec, NodeId> incrementalIds,
                                    TaffyTree freshTree, Map<NodeSpec, NodeId> freshIds,
                                    List<String> failures, Stats stats) {
        String path = parentPath.isEmpty() ? spec.name : parentPath + "/" + spec.name;
        String where = "seed=" + seed + " step=" + step
            + (variant.isEmpty() ? "" : " " + variant) + " node=" + path;

        NodeId incrementalId = incrementalIds.get(spec);
        NodeId freshId = freshIds.get(spec);
        if (incrementalId == null || freshId == null) {
            addFailure(failures, where + " missing node id (incremental=" + incrementalId
                + ", fresh=" + freshId + ")");
            return;
        }

        int expectedChildren = spec.children.size();
        if (incrementalTree.childCount(incrementalId) != expectedChildren) {
            addFailure(failures, where + " childCount: fresh=" + expectedChildren
                + " incremental=" + incrementalTree.childCount(incrementalId));
        }
        stats.nodesCompared++;

        compareLayout(failures, where + " final",
            freshTree.getLayout(freshId), incrementalTree.getLayout(incrementalId));
        compareLayout(failures, where + " unrounded",
            freshTree.getUnroundedLayout(freshId), incrementalTree.getUnroundedLayout(incrementalId));

        for (NodeSpec child : spec.children) {
            compareNode(seed, step, variant, path, child, incrementalTree, incrementalIds,
                freshTree, freshIds, failures, stats);
        }
    }

    private static void compareLayout(List<String> failures, String where, Layout expected, Layout actual) {
        if (expected == null || actual == null) {
            if (expected != actual) {
                addFailure(failures, where + ": fresh=" + expected + " incremental=" + actual);
            }
            return;
        }
        checkInt(failures, where, "order", expected.order(), actual.order());
        checkFloat(failures, where, "location.x", expected.location().x, actual.location().x);
        checkFloat(failures, where, "location.y", expected.location().y, actual.location().y);
        checkFloat(failures, where, "size.width", expected.size().width, actual.size().width);
        checkFloat(failures, where, "size.height", expected.size().height, actual.size().height);
        checkFloat(failures, where, "contentSize.width", expected.contentSize().width, actual.contentSize().width);
        checkFloat(failures, where, "contentSize.height", expected.contentSize().height, actual.contentSize().height);

        ScrollableOverflow expectedOverflow = expected.scrollableOverflow();
        ScrollableOverflow actualOverflow = actual.scrollableOverflow();
        checkFloat(failures, where, "scrollableOverflow.left", expectedOverflow.left(), actualOverflow.left());
        checkFloat(failures, where, "scrollableOverflow.right", expectedOverflow.right(), actualOverflow.right());
        checkFloat(failures, where, "scrollableOverflow.top", expectedOverflow.top(), actualOverflow.top());
        checkFloat(failures, where, "scrollableOverflow.bottom", expectedOverflow.bottom(), actualOverflow.bottom());
        checkBoolean(failures, where, "scrollableOverflow.reversedX",
            expectedOverflow.reversedX(), actualOverflow.reversedX());
        checkBoolean(failures, where, "scrollableOverflow.reversedY",
            expectedOverflow.reversedY(), actualOverflow.reversedY());

        checkFloat(failures, where, "scrollbarSize.width",
            expected.scrollbarSize().width, actual.scrollbarSize().width);
        checkFloat(failures, where, "scrollbarSize.height",
            expected.scrollbarSize().height, actual.scrollbarSize().height);

        compareRect(failures, where, "border", expected.border(), actual.border());
        compareRect(failures, where, "padding", expected.padding(), actual.padding());
        compareRect(failures, where, "margin", expected.margin(), actual.margin());
    }

    private static void compareRect(List<String> failures, String where, String field,
                                    FloatRect expected, FloatRect actual) {
        checkFloat(failures, where, field + ".left", expected.left, actual.left);
        checkFloat(failures, where, field + ".right", expected.right, actual.right);
        checkFloat(failures, where, field + ".top", expected.top, actual.top);
        checkFloat(failures, where, field + ".bottom", expected.bottom, actual.bottom);
    }

    private static void checkFloat(List<String> failures, String where, String field,
                                   float expected, float actual) {
        if (Float.compare(expected, actual) != 0) {
            addFailure(failures, where + " " + field + ": fresh=" + expected + " incremental=" + actual);
        }
    }

    private static void checkInt(List<String> failures, String where, String field, int expected, int actual) {
        if (expected != actual) {
            addFailure(failures, where + " " + field + ": fresh=" + expected + " incremental=" + actual);
        }
    }

    private static void checkBoolean(List<String> failures, String where, String field,
                                     boolean expected, boolean actual) {
        if (expected != actual) {
            addFailure(failures, where + " " + field + ": fresh=" + expected + " incremental=" + actual);
        }
    }

    private static void addFailure(List<String> failures, String message) {
        if (failures.size() < MAX_FAILURES) {
            failures.add(message);
        }
    }

    // === Model ===

    /** The style model shared by the incremental and the freshly built tree. */
    private static final class NodeSpec {
        final String name;
        final int depth;
        final List<NodeSpec> children = new ArrayList<>();
        TaffyStyle style;
        MeasureFunc measure;

        NodeSpec(String name, int depth, TaffyStyle style) {
            this.name = name;
            this.depth = depth;
            this.style = style;
        }
    }

    private static final class Stats {
        int steps;
        int nodesCompared;
        int listenerCalls;
    }
}
