package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.style.TaffyDisplay;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Plain-Java heap-footprint probe for the benchmark tree shapes.
 *
 * <p>This class is deliberately not a JMH benchmark: it never starts JMH. It builds one tree at a
 * time, forces a full GC and reads
 * {@code ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()} at five points of a
 * tree's life, so that {@code docs/perf-memory.md} has a reproducible command and numbers:</p>
 *
 * <ol>
 *   <li><b>baseline</b>: after three {@code System.gc()} calls, before the tree exists;</li>
 *   <li><b>after build</b>: the tree with cold layout caches - node records, styles, child lists
 *       and the tree's dense id arrays;</li>
 *   <li><b>after layout</b>: one cold {@code computeLayout} followed by one warm call, so every
 *       node has its layout records and cache entries (the per-frame steady state);</li>
 *   <li><b>after cache clear</b>: every node's layout cache cleared, the benchmark
 *       {@code mark_all_dirty} path; the residual is what clearing does not free;</li>
 *   <li><b>after {@code tree.clear()}</b>: the node storage is gone; the residual is what the
 *       caller still holds plus GC/sampling noise.</li>
 * </ol>
 *
 * <p>The shapes are the ones the benchmark suite already owns: {@code PhaseSupport.buildFlat}
 * (100k-node flat flex container), {@code Ldlib2UiBenchmark.buildForm} (20k-node LDLib2 form) and
 * {@code PhaseSupport.buildDeep} (100k-node deep flex chain). {@code flat-flex-100k-styled} is
 * the same flat shape with a {@link TaffyStyle#copy() per-leaf style}, which is what a real
 * UI-element tree pays; the benchmark builder shares one leaf style instance to keep timing
 * focused on layout.</p>
 *
 * <p>Every number is a difference of two used-heap samples, not an object-graph measurement.
 * {@code System.gc()} is only a request: the JVM is not required to collect everything, and the
 * sample includes unrelated allocations made between the two reads. The probe repeats the whole
 * life cycle for {@code rounds} rounds and reports median and min/max spread; anything below
 * roughly 1 MiB per 100k nodes is noise. Use {@code jcmd <pid> GC.class_histogram} or
 * {@code jmap -histo:live} when exact object sizes are required.</p>
 *
 * <p>Run it through Gradle (see {@code build.gradle} and {@code docs/perf-memory.md}):</p>
 * <pre>{@code
 * ./gradlew memoryFootprint
 * ./gradlew memoryFootprint -PmemoryFootprintRounds=7 -PmemoryFootprintOnly=flat-flex-100k
 * ./gradlew memoryFootprint -PmemoryFootprintFill=8 -PmemoryFootprintOnly=form-20k
 * }</pre>
 *
 * <p>{@code fill=N} is an optional sixth measurement point: after the first layout it lays the
 * tree out at {@code N} more viewport widths, so the per-node measure cache can fill up to its
 * nine-entry ceiling. This is the expensive worst case a UI that resizes or hovers a lot pays;
 * the default (0) reports only the steady-state cache after one layout.</p>
 *
 * <p>For the old-vs-current comparison the same class is run in a second java process whose
 * classpath is the compiled JMH classes plus the published {@code taffy-1.2.0-local5-all.jar}
 * instead of the current main classes; {@code -Dtaffy.memory.label=...} (or the {@code label=}
 * program argument) tags the report.</p>
 */
public final class MemoryFootprintMain {

    /** Used-heap samples per measurement point; one {@code System.gc()} is often not enough. */
    private static final int GC_SAMPLES = 3;

    /** Pause after every {@code System.gc()} so that a concurrent/concurrent-mark cycle can finish. */
    private static final long GC_SETTLE_MILLIS = 30L;

    private static final int DEFAULT_ROUNDS = 5;

    private static final long MIB = 1024L * 1024L;

    /** Flat shape: one flex container with {@code FLAT_NODES - 1} leaves (PhaseSupport.buildFlat). */
    private static final int FLAT_NODES = 100_000;

    /** Form shape: {@code Ldlib2UiBenchmark.buildForm(20_000)}, the documented LDLib2 form size. */
    private static final int FORM_NODES = 20_000;

    /** Deep shape: a single flex chain with 100k nodes; {@code buildDeep} makes {@code 2 * depth + 2}. */
    private static final int DEEP_NODES = 100_000;
    private static final int DEEP_DEPTH = (DEEP_NODES - 2) / 2;

    private MemoryFootprintMain() {
    }

    public static void main(String[] args) throws InterruptedException {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.out.println("error: " + e.getMessage());
            printUsage();
            return;
        }
        if (options.help) {
            printUsage();
            return;
        }

        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        System.out.println("# memory footprint (plain java, no JMH)");
        System.out.printf(Locale.ROOT, "label=%s%n", options.label);
        System.out.printf(Locale.ROOT, "# java=%s (%s) vm=%s%n",
            System.getProperty("java.version"), System.getProperty("java.vendor"),
            System.getProperty("java.vm.name"));
        System.out.printf(Locale.ROOT, "# os=%s %s, cpus=%d%n",
            System.getProperty("os.name"), System.getProperty("os.arch"),
            Runtime.getRuntime().availableProcessors());
        System.out.printf(Locale.ROOT, "# gc=%s%n", gcNames());
        System.out.printf(Locale.ROOT, "# heap max=%.2f MiB%n", heap.getMax() / (double) MIB);
        System.out.printf(Locale.ROOT,
            "# rounds=%d, sample=used heap after %dx System.gc() (+%d ms settle)%n",
            options.rounds, GC_SAMPLES, GC_SETTLE_MILLIS);
        System.out.println("# caveat: used-heap deltas are approximate retained-size estimates; "
                           + "System.gc() is advisory and the sample includes unrelated JVM state.");

        List<ScenarioResult> results = new ArrayList<>();
        for (Scenario scenario : scenarios()) {
            if (!options.includes(scenario.name)) {
                continue;
            }
            ScenarioResult result = measureScenario(scenario, options.rounds, options.fillCache);
            results.add(result);
            printSummary(result);
        }

        System.out.println();
        printCsv(results, options.label);
        printNotes(results);
    }

    // === Scenarios ========================================================================

    /** A built tree: the fixture is the tree plus the root used for layout and invalidation. */
    private record Fixture(TaffyTree tree, NodeId root) {
    }

    /**
     * One measured tree shape.
     *
     * @param name       stable identifier used by {@code only=} and in the report
     * @param targetNodes requested node count for the report header
     * @param builder    builds a fresh tree; the fixture is dropped after each round
     * @param space      available space passed to {@code computeLayout}
     * @param resizeSpace space for the optional cache-fill layouts; index 0..N-1
     * @param shapeNote  one-line description printed with the scenario
     */
    private record Scenario(String name, int targetNodes, Supplier<Fixture> builder,
                            TaffySize<AvailableSpace> space,
                            IntFunction<TaffySize<AvailableSpace>> resizeSpace,
                            String shapeNote) {
    }

    private static List<Scenario> scenarios() {
        List<Scenario> scenarios = new ArrayList<>();
        scenarios.add(new Scenario(
            "flat-flex-100k", FLAT_NODES, () -> buildFlatFlex(FLAT_NODES),
            PhaseSupport.layoutSpace(),
            index -> TaffySize.of(AvailableSpace.definite(1000f + index * 100f), AvailableSpace.maxContent()),
            "PhaseSupport.buildFlat(flex, 100000, fixed): one container + 99,999 leaves; all leaves share one TaffyStyle"));
        scenarios.add(new Scenario(
            "flat-flex-100k-styled", FLAT_NODES, () -> buildFlatFlexStyled(FLAT_NODES),
            PhaseSupport.layoutSpace(),
            index -> TaffySize.of(AvailableSpace.definite(1000f + index * 100f), AvailableSpace.maxContent()),
            "same flat shape, but every leaf gets its own TaffyStyle.copy(), i.e. the per-element style a real UI tree owns"));
        scenarios.add(new Scenario(
            "form-20k", FORM_NODES, () -> buildForm(FORM_NODES),
            Ldlib2UiBenchmark.space(Ldlib2UiBenchmark.FORM_WIDTH, Ldlib2UiBenchmark.FORM_HEIGHT),
            index -> Ldlib2UiBenchmark.space(1600f + index * 40f, Ldlib2UiBenchmark.FORM_HEIGHT),
            "Ldlib2UiBenchmark.buildForm(20000): 50 block panels x 100 flex rows x 3 measured labels"));
        scenarios.add(new Scenario(
            "deep-100k", DEEP_NODES, MemoryFootprintMain::buildDeep,
            PhaseSupport.layoutSpace(),
            index -> TaffySize.of(AvailableSpace.definite(1000f + index * 100f), AvailableSpace.maxContent()),
            "PhaseSupport.buildDeep(flex, depth=49,999): a single 100,000-node chain; layout recurses ~50k frames (-Xss512m)"));
        return scenarios;
    }

    private static Fixture buildFlatFlex(int nodeCount) {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        return new Fixture(tree, PhaseSupport.buildFlat(tree, "flex", nodeCount, PhaseSupport.FIXED));
    }

    /** The flat shape with one style instance per leaf, like a real element tree (no style sharing). */
    private static Fixture buildFlatFlexStyled(int nodeCount) {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        TaffyStyle leaf = PhaseSupport.leafStyle(PhaseSupport.FIXED, TaffyDisplay.FLEX);
        NodeId[] children = new NodeId[nodeCount - 1];
        for (int i = 0; i < children.length; i++) {
            children[i] = tree.newLeaf(leaf.copy());
        }
        NodeId root = tree.newWithChildren(
            PhaseSupport.flatRootStyle(TaffyDisplay.FLEX, PhaseSupport.FIXED), children);
        return new Fixture(tree, root);
    }

    private static Fixture buildForm(int nodeCount) {
        Ldlib2UiBenchmark.FormShape shape = Ldlib2UiBenchmark.buildForm(nodeCount);
        return new Fixture(shape.tree, shape.root);
    }

    private static Fixture buildDeep() {
        TaffyTree tree = new TaffyTree();
        tree.disableRounding();
        return new Fixture(tree, PhaseSupport.buildDeep(tree, "flex", DEEP_DEPTH, PhaseSupport.FIXED));
    }

    // === Measurement ======================================================================

    /** Raw used-heap samples of one tree life cycle, in bytes. */
    private record Round(int nodes, long base, long build, long layout, long cacheFull,
                         long cacheClear, long treeClear) {
        long buildBytes() {
            return build - base;
        }

        long layoutBytes() {
            return layout - build;
        }

        /** Extra heap the cache-fill layouts added on top of the first layout (0 when disabled). */
        long cacheFullExtra() {
            return cacheFull - layout;
        }

        long cacheClearResidual() {
            return cacheClear - base;
        }

        long treeClearResidual() {
            return treeClear - base;
        }
    }

    private static ScenarioResult measureScenario(Scenario scenario, int rounds, int fillCache) {
        System.out.println();
        System.out.printf(Locale.ROOT, "scenario=%s targetNodes=%d space=%s%n",
            scenario.name, scenario.targetNodes, describe(scenario.space));
        System.out.printf(Locale.ROOT, "# shape: %s%n", scenario.shapeNote);

        // Warm-up: load classes, JIT the builders and the layout path, and prove the deep shape's
        // stack requirement before measured rounds. The warm-up tree is dropped and collected.
        warmUp(scenario);
        usedHeapAfterGc();

        long[] build = new long[rounds];
        long[] layout = new long[rounds];
        long[] cacheFull = new long[fillCache > 0 ? rounds : 0];
        long[] cacheClear = new long[rounds];
        long[] treeClear = new long[rounds];
        long[] drop = new long[rounds];
        int nodes = 0;

        for (int round = 1; round <= rounds; round++) {
            Round sample = measureRound(scenario, fillCache);
            nodes = sample.nodes;
            // The fixture local died when measureRound returned, so this GC sees only the
            // caller-side state: the real "did the tree memory come back" check.
            long afterDrop = usedHeapAfterGc();

            build[round - 1] = sample.buildBytes();
            layout[round - 1] = sample.layoutBytes();
            if (fillCache > 0) {
                cacheFull[round - 1] = sample.cacheFullExtra();
            }
            cacheClear[round - 1] = sample.cacheClearResidual();
            treeClear[round - 1] = sample.treeClearResidual();
            drop[round - 1] = afterDrop - sample.base;

            if (fillCache > 0) {
                System.out.printf(Locale.ROOT,
                    "  round %d (nodes=%d): base=%.2f MiB | tree=%s (%s/node) | layout=%s (%s/node)"
                    + " | cacheFull +%d resizes=%s (%s/node) | cacheClear residual=%s (%s/node)"
                    + " | treeClear residual=%s | drop residual=%s%n",
                    round, sample.nodes, sample.base / (double) MIB,
                    delta(sample.buildBytes()), perNode(sample.buildBytes(), sample.nodes),
                    delta(sample.layoutBytes()), perNode(sample.layoutBytes(), sample.nodes),
                    fillCache, delta(sample.cacheFullExtra()),
                    perNode(sample.cacheFullExtra(), sample.nodes),
                    delta(sample.cacheClearResidual()), perNode(sample.cacheClearResidual(), sample.nodes),
                    delta(sample.treeClearResidual()), delta(afterDrop - sample.base));
            } else {
                System.out.printf(Locale.ROOT,
                    "  round %d (nodes=%d): base=%.2f MiB | tree=%s (%s/node) | layout=%s (%s/node)"
                    + " | cacheClear residual=%s (%s/node) | treeClear residual=%s | drop residual=%s%n",
                    round, sample.nodes, sample.base / (double) MIB,
                    delta(sample.buildBytes()), perNode(sample.buildBytes(), sample.nodes),
                    delta(sample.layoutBytes()), perNode(sample.layoutBytes(), sample.nodes),
                    delta(sample.cacheClearResidual()), perNode(sample.cacheClearResidual(), sample.nodes),
                    delta(sample.treeClearResidual()), delta(afterDrop - sample.base));
            }
        }

        return new ScenarioResult(scenario.name, nodes, rounds, build, layout, cacheFull,
            cacheClear, treeClear, drop);
    }

    private static Round measureRound(Scenario scenario, int fillCache) {
        long base = usedHeapAfterGc();

        Fixture fixture = scenario.builder.get();
        int nodes = fixture.tree.totalNodeCount();
        long build = usedHeapAfterGc();

        fixture.tree.computeLayout(fixture.root, scenario.space);
        // A second identical request hits the tree-clean fast path: this is the steady state an
        // idle frame sees, and it must not add any new cache entries.
        fixture.tree.computeLayout(fixture.root, scenario.space);
        long layout = usedHeapAfterGc();

        // Optional worst case: distinct viewport widths fill the nine per-node measure entries.
        long cacheFull = layout;
        for (int i = 0; i < fillCache; i++) {
            fixture.tree.computeLayout(fixture.root, scenario.resizeSpace.apply(i));
        }
        if (fillCache > 0) {
            cacheFull = usedHeapAfterGc();
        }

        clearAllCaches(fixture.tree, fixture.root);
        long cacheClear = usedHeapAfterGc();

        fixture.tree.clear();
        long treeClear = usedHeapAfterGc();

        return new Round(nodes, base, build, layout, cacheFull, cacheClear, treeClear);
    }

    /**
     * Builds a throwaway tree of the scenario shape, lays it out once, clears it and drops it.
     *
     * <p>Class loading, JIT compilation and the first cache allocations are not part of the
     * reported numbers; the warm-up tree dies before the first measured baseline.</p>
     */
    private static void warmUp(Scenario scenario) {
        Fixture fixture = scenario.builder.get();
        fixture.tree.computeLayout(fixture.root, scenario.space);
        clearAllCaches(fixture.tree, fixture.root);
        fixture.tree.clear();
    }

    /**
     * Clears every node's layout cache with an iterative walk.
     *
     * <p>The benchmark helper {@code BenchSupport.clearCaches} needs a pre-collected
     * {@code NodeId[]}; collecting it here would add ~4 B/node to the measured tree, so this
     * method walks the tree with a reusable deque instead. The deque is local and dies before the
     * sample is taken.</p>
     */
    private static void clearAllCaches(TaffyTree tree, NodeId root) {
        ArrayDeque<NodeId> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            NodeId node = queue.poll();
            tree.clearCache(node);
            for (NodeId child : tree.getChildren(node)) {
                queue.add(child);
            }
        }
    }

    /** {@code System.gc()} three times, then the used heap in bytes. */
    private static long usedHeapAfterGc() {
        for (int i = 0; i < GC_SAMPLES; i++) {
            System.gc();
            try {
                Thread.sleep(GC_SETTLE_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for GC", e);
            }
        }
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    // === Reporting ========================================================================

    private record ScenarioResult(String name, int nodes, int rounds,
                                  long[] build, long[] layout, long[] cacheFull,
                                  long[] cacheClear, long[] treeClear, long[] drop) {
    }

    private static void printSummary(ScenarioResult result) {
        long buildMedian = median(result.build);
        long layoutMedian = median(result.layout);
        long cacheClearMedian = median(result.cacheClear);
        long treeClearMedian = median(result.treeClear);
        long dropMedian = median(result.drop);

        System.out.printf(Locale.ROOT,
            "  median (nodes=%d): tree=%s (%s/node) | layout=%s (%s/node)"
            + " | cacheClear residual=%s (%s/node) | treeClear residual=%s | drop residual=%s%n",
            result.nodes,
            delta(buildMedian), perNode(buildMedian, result.nodes),
            delta(layoutMedian), perNode(layoutMedian, result.nodes),
            delta(cacheClearMedian), perNode(cacheClearMedian, result.nodes),
            delta(treeClearMedian), delta(dropMedian));
        if (result.cacheFull.length > 0) {
            long cacheFullMedian = median(result.cacheFull);
            System.out.printf(Locale.ROOT,
                "  cacheFull median: %s (%s/node) extra after the resize layouts; total warm=%s (%s/node)%n",
                delta(cacheFullMedian), perNode(cacheFullMedian, result.nodes),
                delta(buildMedian + layoutMedian + cacheFullMedian),
                perNode(buildMedian + layoutMedian + cacheFullMedian, result.nodes));
        }
        System.out.printf(Locale.ROOT,
            "  noise: tree spread %s, layout spread %s, cacheClear spread %s, treeClear spread %s%n",
            delta(spread(result.build)), delta(spread(result.layout)),
            delta(spread(result.cacheClear)), delta(spread(result.treeClear)));
    }

    private static void printCsv(List<ScenarioResult> results, String label) {
        System.out.println("# csv");
        System.out.println("label,scenario,nodes,stage,median_bytes,median_per_node,min_bytes,max_bytes");
        for (ScenarioResult result : results) {
            csvRow(label, result, "tree", result.build);
            csvRow(label, result, "layout", result.layout);
            if (result.cacheFull.length > 0) {
                csvRow(label, result, "layoutCacheFullExtra", result.cacheFull);
            }
            csvRow(label, result, "cacheClearResidual", result.cacheClear);
            csvRow(label, result, "treeClearResidual", result.treeClear);
            csvRow(label, result, "dropResidual", result.drop);
        }
    }

    private static void csvRow(String label, ScenarioResult result, String stage, long[] values) {
        long median = median(values);
        System.out.printf(Locale.ROOT, "%s,%s,%d,%s,%d,%s,%d,%d%n",
            label, result.name, result.nodes, stage, median, perNode(median, result.nodes),
            min(values), max(values));
    }

    private static void printNotes(List<ScenarioResult> results) {
        System.out.println();
        ScenarioResult shared = find(results, "flat-flex-100k");
        ScenarioResult styled = find(results, "flat-flex-100k-styled");
        if (shared != null && styled != null) {
            long sharedBuild = median(shared.build);
            long styledBuild = median(styled.build);
            long styleBytes = styledBuild - sharedBuild;
            System.out.printf(Locale.ROOT,
                "# style cost: per-leaf TaffyStyle.copy() adds %s (%s/node) on the 100k flat shape;"
                + " use the styled line as the realistic per-node number.%n",
                delta(styleBytes), perNode(styleBytes, styled.nodes));
        }
        if (results.stream().anyMatch(result -> result.cacheFull.length > 0)) {
            System.out.println("# cacheFull is opt-in (fill=N): the extra heap after N more viewport widths,"
                               + " i.e. the measure-cache ceiling a resizing UI can reach.");
        }
        System.out.println("# deep-100k requires -Xss512m (the Gradle task sets it); the recursion depth is ~50k frames.");
        System.out.println("# form-20k includes the detached row built by Ldlib2UiBenchmark.buildForm and the");
        System.out.println("# fixture's own NodeId[] (~4 B/node), i.e. it is an upper bound of the form tree itself.");
    }

    private static ScenarioResult find(List<ScenarioResult> results, String name) {
        for (ScenarioResult result : results) {
            if (result.name.equals(name)) {
                return result;
            }
        }
        return null;
    }

    private static String gcNames() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .map(GarbageCollectorMXBean::getName)
            .collect(Collectors.joining(", "));
    }

    private static String describe(TaffySize<AvailableSpace> space) {
        return space.width + " x " + space.height;
    }

    private static String delta(long bytes) {
        return String.format(Locale.ROOT, "%+.2f MiB", bytes / (double) MIB);
    }

    private static String perNode(long bytes, int nodes) {
        if (nodes <= 0) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%d B", Math.round((double) bytes / nodes));
    }

    private static long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 1) {
            return sorted[middle];
        }
        return Math.round((sorted[middle - 1] + sorted[middle]) / 2.0);
    }

    private static long min(long[] values) {
        long min = Long.MAX_VALUE;
        for (long value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private static long max(long[] values) {
        long max = Long.MIN_VALUE;
        for (long value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static long spread(long[] values) {
        return max(values) - min(values);
    }

    // === Command line =====================================================================

    /** Parsed command line: {@code rounds=N only=a,b label=name}. */
    private static final class Options {
        int rounds = DEFAULT_ROUNDS;
        int fillCache;
        String label = System.getProperty("taffy.memory.label", "current");
        String only;
        boolean help;

        boolean includes(String scenarioName) {
            if (only == null) {
                return true;
            }
            for (String token : only.split(",")) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty() && (scenarioName.equals(trimmed) || scenarioName.startsWith(trimmed))) {
                    return true;
                }
            }
            return false;
        }

        static Options parse(String[] args) {
            Options options = new Options();
            for (String arg : args) {
                String key = arg;
                String value = "";
                int equals = arg.indexOf('=');
                if (equals >= 0) {
                    key = arg.substring(0, equals);
                    value = arg.substring(equals + 1);
                }
                while (key.startsWith("-")) {
                    key = key.substring(1);
                }
                switch (key) {
                    case "rounds" -> options.rounds = parseRounds(value, "rounds", false);
                    case "fill" -> options.fillCache = parseRounds(value, "fill", true);
                    case "only" -> options.only = value;
                    case "label" -> options.label = value;
                    case "help", "h" -> options.help = true;
                    default -> throw new IllegalArgumentException("unknown argument: " + arg);
                }
            }
            return options;
        }

        private static int parseRounds(String value, String name, boolean allowZero) {
            int parsed;
            try {
                parsed = Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(name + " must be a non-negative integer, got: " + value);
            }
            if (parsed < (allowZero ? 0 : 1)) {
                throw new IllegalArgumentException(name + " must be >= " + (allowZero ? 0 : 1)
                                                   + ", got: " + parsed);
            }
            return parsed;
        }
    }

    private static void printUsage() {
        System.out.println("usage: MemoryFootprintMain [rounds=" + DEFAULT_ROUNDS + "]"
                           + " [fill=0] [only=flat-flex-100k,flat-flex-100k-styled,form-20k,deep-100k]"
                           + " [label=current]");
        System.out.println("  fill=N adds N extra layouts at other viewport widths to fill the per-node caches.");
        System.out.println("  label also defaults to -Dtaffy.memory.label=...; only accepts prefixes.");
    }
}
