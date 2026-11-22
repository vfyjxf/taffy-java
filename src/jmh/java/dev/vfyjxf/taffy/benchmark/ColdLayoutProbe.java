package dev.vfyjxf.taffy.benchmark;

import dev.vfyjxf.taffy.geometry.TaffySize;
import dev.vfyjxf.taffy.style.AvailableSpace;
import dev.vfyjxf.taffy.tree.NodeId;
import dev.vfyjxf.taffy.tree.TaffyTree;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Plain-Java true-cold layout probe.
 *
 * <p>The JMH suite calls {@link dev.vfyjxf.taffy.tree.TaffyTree#clearCache(NodeId)} on every node
 * in {@code @Setup(Level.Invocation)} and calls the result "cold layout". That reproduces
 * upstream taffy's {@code mark_all_dirty}: every node's {@code LayoutCache} is emptied, but the
 * tree itself, its styles and its measure callbacks stay alive. The cross-pass item-list caches
 * added after p17 are stored on the node, not in the layout cache: {@code clearCache} clears the
 * block item list, while the p20 flex item list and the p22 grid item list survive. Consequently
 * the second and all later JMH invocations reuse item metadata built by the first layout, which is
 * not what "first frame after creating the UI" costs.</p>
 *
 * <p>This probe measures the two semantics separately on the same compiled code:</p>
 *
 * <ul>
 *   <li><b>true-cold</b>: a brand-new {@link TaffyTree}, styles and measure callbacks are built for
 *       every round; only {@link TaffyTree#computeLayout} is timed. Before the timer starts the
 *       freshly built tree is kept alive and a full GC removes the previous round's garbage. This
 *       is the first-frame cost of a UI tree.</li>
 *   <li><b>clear-cache</b>: one tree is built and laid out once, then every round calls
 *       {@code clearCache} on every node before the timed {@code computeLayout}. This reproduces
 *       the JMH cold-layout convention, including the surviving flex/grid item caches.</li>
 * </ul>
 *
 * <p>Every shape is measured in {@code repeats} independent blocks of {@code rounds} rounds; the
 * report includes the median over all rounds, min/max, the spread, and the median of each block.
 * The 100k-node and superDeep shapes default to {@code largeRounds} (10) because one round costs
 * 50-500 ms; everything else defaults to 20 rounds. Small shapes like {@code measure-2000} are
 * sensitive to JIT and GC; use at least two repeats and more rounds when deciding. The probe is a
 * plain {@code main} class and never starts JMH.</p>
 *
 * <p>Current build:</p>
 * <pre>{@code
 * ./gradlew coldLayoutProbe
 * ./gradlew coldLayoutProbe -PcoldProbeRounds=30 -PcoldProbeRepeats=3
 * ./gradlew coldLayoutProbe -PcoldProbeOnly=wideGrid-316 -PcoldProbeMode=true
 * }</pre>
 *
 * <p>Old published jar: the same compiled probe and benchmark classes run against
 * {@code taffy-1.2.0-local5-all.jar} by replacing only the main jar on the classpath. The probe
 * source never references anything the old jar lacks; the grid shape builders are reached by
 * reflection because they are private in {@code GridBenchmark}.</p>
 * <pre>{@code
 * ./gradlew coldLayoutProbe \
 *   -PcoldProbeOldJar="$HOME/.m2/repository/dev/vfyjxf/taffy/1.2.0-local5/taffy-1.2.0-local5-all.jar" \
 *   -PcoldProbeLabel=local5
 * }</pre>
 *
 * <p>Do not run this probe concurrently with anything else: the 100k shapes and their full GCs
 * make timings very sensitive to other CPU/memory activity.</p>
 */
public final class ColdLayoutProbe {

    private static final int DEFAULT_ROUNDS = 20;
    private static final int DEFAULT_LARGE_ROUNDS = 10;
    private static final int DEFAULT_WARMUP = 2;
    private static final int DEFAULT_WARMUP_MILLIS = 1500;
    private static final int DEFAULT_REPEATS = 2;
    private static final long GC_SETTLE_MILLIS = 10L;

    /** Whether to run a full GC between rounds, outside the timer. */
    private static boolean gcBeforeTimer = true;

    /** Accumulates layout results so the JIT cannot discard a timed call. */
    private static double sink;

    private ColdLayoutProbe() {
    }

    public static void main(String[] args) throws Exception {
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
        gcBeforeTimer = options.gc;

        System.out.println("# true-cold vs clear-cache layout probe (plain java, no JMH)");
        System.out.printf(Locale.ROOT, "label=%s%n", options.label);
        System.out.printf(Locale.ROOT, "# java=%s (%s) vm=%s%n",
            System.getProperty("java.version"), System.getProperty("java.vendor"),
            System.getProperty("java.vm.name"));
        System.out.printf(Locale.ROOT, "# os=%s %s, cpus=%d%n",
            System.getProperty("os.name"), System.getProperty("os.arch"),
            Runtime.getRuntime().availableProcessors());
        System.out.printf(Locale.ROOT, "# taffy classes: %s%n", codeSource(TaffyTree.class));
        System.out.printf(Locale.ROOT, "# probe classes: %s%n", codeSource(ColdLayoutProbe.class));
        System.out.printf(Locale.ROOT,
            "# rounds=%d (large=%d), warmup>=%d rounds or %d ms, repeats=%d, gc-before-timer=%s (+%d ms settle)%n",
            options.rounds, options.largeRounds, options.warmup, options.warmupMillis, options.repeats,
            options.gc ? "yes" : "no", GC_SETTLE_MILLIS);
        System.out.println("# true=rebuild tree per round, time computeLayout only");
        System.out.println("# clear=build once, layout once, clearCache every round (JMH cold semantics)");
        System.out.println("# caveat: full GCs run outside the timer; shapes below ~0.2 ms are dominated by noise");

        List<ShapeResult> results = new ArrayList<>();
        for (Shape shape : shapes()) {
            if (!options.includes(shape.name)) {
                continue;
            }
            ShapeResult result = measureShape(shape, options);
            results.add(result);
            printShape(result);
        }

        System.out.println();
        printSummary(results, options.label);
        System.out.println();
        printCsv(results, options.label);
        System.out.printf(Locale.ROOT, "sink=%.6f%n", sink);
    }

    private static String codeSource(Class<?> type) {
        var source = type.getProtectionDomain().getCodeSource();
        return source == null || source.getLocation() == null ? "<unknown>" : source.getLocation().toString();
    }

    // === Shapes ==========================================================================

    /** A tree plus its root; the fixture is rebuilt for every true-cold round. */
    private record Fixture(TaffyTree tree, NodeId root) {
    }

    /**
     * One measured shape.
     *
     * @param name   stable identifier used by {@code only=} and in the report
     * @param large  whether the shape uses {@code largeRounds} instead of {@code rounds}
     * @param builder builds a fresh tree, styles and measure callbacks
     * @param space  available space passed to {@code computeLayout}
     * @param note   one-line shape description printed with the result
     */
    private record Shape(String name, boolean large, Supplier<Fixture> builder,
                         TaffySize<AvailableSpace> space, String note) {
    }

    private static List<Shape> shapes() {
        List<Shape> shapes = new ArrayList<>();

        shapes.add(new Shape(
            "form-20k", false,
            () -> fromBase(Ldlib2UiBenchmark.buildForm(20_000)),
            Ldlib2UiBenchmark.space(Ldlib2UiBenchmark.FORM_WIDTH, Ldlib2UiBenchmark.FORM_HEIGHT),
            "Ldlib2UiBenchmark.buildForm(20000): block root, block panels, flex rows, measured labels"));

        shapes.add(new Shape(
            "dashboard-16", false,
            () -> fromBase(Ldlib2UiBenchmark.buildDashboard(16)),
            Ldlib2UiBenchmark.space(Ldlib2UiBenchmark.DASHBOARD_WIDTH, Ldlib2UiBenchmark.DASHBOARD_HEIGHT),
            "Ldlib2UiBenchmark.buildDashboard(16): 16x16 1fr grid of flex cards with abspos badges"));

        shapes.add(new Shape(
            "measure-2000", false,
            () -> fromBase(Ldlib2UiBenchmark.buildMeasureTree(2_000)),
            Ldlib2UiBenchmark.measureSpace(),
            "Ldlib2UiBenchmark.buildMeasureTree(2000): column flex root of measured labels"));

        shapes.add(new Shape(
            "flat-flex-100k", true,
            () -> {
                TaffyTree tree = new TaffyTree();
                tree.disableRounding();
                return new Fixture(tree, PhaseSupport.buildFlat(tree, "flex", 100_000, PhaseSupport.FIXED));
            },
            PhaseSupport.layoutSpace(),
            "PhaseSupport.buildFlat(flex, 100000, fixed), rounding off (PhaseLayoutBenchmark.flatFixed)"));

        shapes.add(new Shape(
            "flat-grid-100k", true,
            () -> {
                TaffyTree tree = new TaffyTree();
                tree.disableRounding();
                return new Fixture(tree, PhaseSupport.buildFlat(tree, "grid", 100_000, PhaseSupport.FIXED));
            },
            PhaseSupport.layoutSpace(),
            "PhaseSupport.buildFlat(grid, 100000, fixed), 64 auto columns, rounding off"));

        shapes.add(new Shape(
            "superDeepGrid-1000", true,
            () -> fromGrid("buildTaffyDeepGridHierarchy", 1_000, 1),
            TaffySize.maxContent(),
            "GridBenchmark grid/superdeep: 1000 nested 1x1 grids, rounding on"));

        shapes.add(new Shape(
            "deepGrid-3_4", false,
            () -> fromGrid("buildTaffyDeepGridHierarchy", 4, 3),
            TaffySize.of(AvailableSpace.definite(12_000f), AvailableSpace.definite(12_000f)),
            "GridBenchmark grid/deep 3_4: 3x3 grids, 4 levels, rounding on"));

        shapes.add(new Shape(
            "wideGrid-316", true,
            () -> fromGrid("buildGridFlatHierarchy", 316, 316),
            TaffySize.of(AvailableSpace.definite(12_000f), AvailableSpace.definite(12_000f)),
            "GridBenchmark grid/wide 316: 316x316 random-track grid, rounding on"));

        return shapes;
    }

    private static Fixture fromBase(Ldlib2UiBenchmark.BaseShape shape) {
        return new Fixture(shape.tree, shape.root);
    }

    /**
     * The grid builders in {@link GridBenchmark} are private because they are implementation
     * details of that class. Reflection keeps the probe from editing the benchmark class under
     * measurement; the invocation happens once per tree, outside every timed region.
     */
    private static Fixture fromGrid(String builder, int first, int second) {
        try {
            Method method = GridBenchmark.class.getDeclaredMethod(builder, int.class, int.class);
            method.setAccessible(true);
            Object[] result = (Object[]) method.invoke(null, first, second);
            return new Fixture((TaffyTree) result[0], (NodeId) result[1]);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot invoke GridBenchmark." + builder, e);
        }
    }

    // === Measurement =====================================================================

    private enum Mode {
        TRUE_COLD("true"),
        CLEAR_CACHE("clear");

        final String label;

        Mode(String label) {
            this.label = label;
        }
    }

    private record Run(Mode mode, double[] layoutMs, double[] buildMs, float checksum) {
    }

    private record Stats(double median, double min, double max, double mean, double stddev) {
        static double medianOf(double[] values) {
            double[] sorted = values.clone();
            Arrays.sort(sorted);
            return median(sorted);
        }

        static Stats of(double[] values) {
            double[] sorted = values.clone();
            Arrays.sort(sorted);
            double sum = 0.0;
            for (double value : sorted) {
                sum += value;
            }
            double mean = sum / sorted.length;
            double variance = 0.0;
            for (double value : sorted) {
                double diff = value - mean;
                variance += diff * diff;
            }
            variance /= sorted.length;
            return new Stats(
                median(sorted),
                sorted[0],
                sorted[sorted.length - 1],
                mean,
                Math.sqrt(variance)
            );
        }

        double spread() {
            return max - min;
        }

        private static double median(double[] sorted) {
            int middle = sorted.length / 2;
            return (sorted.length & 1) == 0
                   ? (sorted[middle - 1] + sorted[middle]) / 2.0
                   : sorted[middle];
        }
    }

    private record RunSummary(Mode mode, Stats stats, double prepMedian, float checksum,
                              double[] repeatMedians) {
    }

    private record ShapeResult(String name, String note, int nodes, List<RunSummary> runs) {
        RunSummary trueColdOrNull() {
            return run(Mode.TRUE_COLD);
        }

        RunSummary clearCacheOrNull() {
            return run(Mode.CLEAR_CACHE);
        }

        private RunSummary run(Mode mode) {
            return runs.stream().filter(run -> run.mode() == mode).findFirst().orElse(null);
        }
    }

    private static ShapeResult measureShape(Shape shape, Options options) {
        // Class loading, JIT and first cache allocations happen before the measured runs. The
        // prototype is dropped when it goes out of scope; the explicit GC below collects it.
        Fixture prototype = shape.builder.get();
        int nodes = prototype.tree.totalNodeCount();
        prototype.tree.computeLayout(prototype.root, shape.space);
        float prototypeChecksum = rootChecksum(prototype);

        // The clear-cache tree is built once and kept; its flex/grid item caches must survive the
        // per-round clearCache exactly as they do in the JMH states.
        Fixture clearFixture = shape.builder.get();
        clearFixture.tree.computeLayout(clearFixture.root, shape.space);
        NodeId[] clearNodes = BenchSupport.collectNodes(clearFixture.tree, clearFixture.root);

        boolean measureTrue = !options.mode.equals("clear");
        boolean measureClear = !options.mode.equals("true");

        // Tiered compilation needs hundreds of invocations for the small shapes and several for the
        // 100k ones; a fixed small round count is why the first probe runs looked 5x slower than
        // the JMH scores. Warm until the wall-clock budget and the minimum round count are met.
        long warmupDeadline = System.nanoTime() + options.warmupMillis * 1_000_000L;
        int trueWarmups = 0;
        int clearWarmups = 0;
        while (true) {
            boolean beforeDeadline = System.nanoTime() < warmupDeadline;
            boolean needTrue = measureTrue && (beforeDeadline || trueWarmups < options.warmup);
            boolean needClear = measureClear && (beforeDeadline || clearWarmups < options.warmup);
            if (!needTrue && !needClear) {
                break;
            }
            if (needTrue) {
                Fixture fresh = shape.builder.get();
                fresh.tree.computeLayout(fresh.root, shape.space);
                sink += rootChecksum(fresh);
                fresh = null;
                trueWarmups++;
            }
            if (needClear) {
                BenchSupport.clearCaches(clearFixture.tree, clearNodes);
                clearFixture.tree.computeLayout(clearFixture.root, shape.space);
                sink += rootChecksum(clearFixture);
                clearWarmups++;
            }
        }

        int rounds = options.roundsFor(shape);
        List<Run> runs = new ArrayList<>();
        for (int repeat = 0; repeat < options.repeats; repeat++) {
            // Alternate the order so the second mode of a repeat does not systematically run with a
            // hotter JIT or a different heap shape than the first.
            boolean trueFirst = (repeat & 1) == 0;
            if (measureTrue && (trueFirst || !measureClear)) {
                runs.add(runTrueCold(shape, rounds));
            }
            if (measureClear) {
                runs.add(runClearCache(shape, clearFixture, clearNodes, rounds));
            }
            if (measureTrue && !trueFirst && measureClear) {
                runs.add(runTrueCold(shape, rounds));
            }
        }

        List<RunSummary> summaries = new ArrayList<>();
        for (Mode mode : Mode.values()) {
            List<Double> layouts = new ArrayList<>();
            List<Double> builds = new ArrayList<>();
            List<Double> repeatMedians = new ArrayList<>();
            float checksum = Float.NaN;
            for (Run run : runs) {
                if (run.mode() != mode) {
                    continue;
                }
                for (double value : run.layoutMs) {
                    layouts.add(value);
                }
                for (double value : run.buildMs) {
                    builds.add(value);
                }
                repeatMedians.add(Stats.medianOf(run.layoutMs));
                checksum = run.checksum;
            }
            if (layouts.isEmpty()) {
                continue;
            }
            double[] layoutValues = layouts.stream().mapToDouble(Double::doubleValue).toArray();
            double[] buildValues = builds.stream().mapToDouble(Double::doubleValue).toArray();
            summaries.add(new RunSummary(
                mode,
                Stats.of(layoutValues),
                Stats.of(buildValues).median,
                checksum,
                repeatMedians.stream().mapToDouble(Double::doubleValue).toArray()
            ));
        }

        // Sanity: all measured modes must produce the same root layout for this shape.
        for (RunSummary summary : summaries) {
            if (Float.floatToIntBits(summary.checksum) != Float.floatToIntBits(prototypeChecksum)) {
                throw new IllegalStateException(shape.name + ": layout checksum differs between modes");
            }
        }

        clearFixture = null;
        System.gc();
        return new ShapeResult(shape.name, shape.note, nodes, summaries);
    }

    private static Run runTrueCold(Shape shape, int rounds) {
        double[] layoutMs = new double[rounds];
        double[] buildMs = new double[rounds];
        float checksum = Float.NaN;

        for (int round = 0; round < rounds; round++) {
            long buildStart = System.nanoTime();
            Fixture fresh = shape.builder.get();
            long buildEnd = System.nanoTime();

            // The tree must survive the GC; the previous round's tree and the build temporaries do
            // not. This keeps the heap state at the timer start comparable between the two modes.
            gcBeforeTimer();

            long layoutStart = System.nanoTime();
            fresh.tree.computeLayout(fresh.root, shape.space);
            long layoutEnd = System.nanoTime();

            buildMs[round] = (buildEnd - buildStart) / 1_000_000.0;
            layoutMs[round] = (layoutEnd - layoutStart) / 1_000_000.0;
            checksum = stableChecksum(checksum, rootChecksum(fresh));
            fresh = null;
        }
        return new Run(Mode.TRUE_COLD, layoutMs, buildMs, checksum);
    }

    private static Run runClearCache(Shape shape, Fixture fixture, NodeId[] nodes, int rounds) {
        double[] layoutMs = new double[rounds];
        double[] buildMs = new double[rounds];
        float checksum = Float.NaN;

        for (int round = 0; round < rounds; round++) {
            long buildStart = System.nanoTime();
            BenchSupport.clearCaches(fixture.tree, nodes);
            long buildEnd = System.nanoTime();

            gcBeforeTimer();

            long layoutStart = System.nanoTime();
            fixture.tree.computeLayout(fixture.root, shape.space);
            long layoutEnd = System.nanoTime();

            buildMs[round] = (buildEnd - buildStart) / 1_000_000.0;
            layoutMs[round] = (layoutEnd - layoutStart) / 1_000_000.0;
            checksum = stableChecksum(checksum, rootChecksum(fixture));
        }
        return new Run(Mode.CLEAR_CACHE, layoutMs, buildMs, checksum);
    }

    /** Keeps the first checksum and rejects a run whose layout changed between rounds. */
    private static float stableChecksum(float expected, float actual) {
        if (Float.isNaN(expected)) {
            return actual;
        }
        if (Float.floatToIntBits(expected) != Float.floatToIntBits(actual)) {
            throw new IllegalStateException("layout checksum changed between rounds: " + expected + " -> " + actual);
        }
        return expected;
    }

    private static float rootChecksum(Fixture fixture) {
        var layout = fixture.tree.getLayout(fixture.root);
        if (layout == null) {
            throw new IllegalStateException("computeLayout produced no root layout");
        }
        return layout.location().x * 1.25f
               + layout.location().y * 1.5f
               + layout.size().width * 0.75f
               + layout.size().height;
    }

    private static void gcBeforeTimer() {
        if (!gcBeforeTimer) {
            return;
        }
        System.gc();
        sleep(GC_SETTLE_MILLIS);
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted between rounds", e);
        }
    }

    // === Reporting =======================================================================

    private static void printShape(ShapeResult result) {
        System.out.println();
        System.out.printf(Locale.ROOT, "scenario=%s nodes=%d%n", result.name, result.nodes);
        System.out.printf(Locale.ROOT, "# shape: %s%n", result.note);
        System.out.println("  mode      median_ms   min_ms   max_ms  spread_ms   mean_ms  stddev_ms  prep_med_ms");
        for (RunSummary run : result.runs) {
            Stats stats = run.stats;
            System.out.printf(Locale.ROOT,
                "  %-8s %10.4f %8.4f %8.4f %10.4f %9.4f %10.4f %13.4f%n",
                run.mode.label, stats.median, stats.min, stats.max, stats.spread(),
                stats.mean, stats.stddev, run.prepMedian);
        }
        if (result.trueColdOrNull() != null && result.clearCacheOrNull() != null) {
            double trueCold = result.trueColdOrNull().stats.median;
            double clear = result.clearCacheOrNull().stats.median;
            System.out.printf(Locale.ROOT, "  true-cold vs clear-cache: %+.2f%% (clear is %s)%n",
                percent(trueCold, clear), clear <= trueCold ? "faster" : "slower");
        }
        for (RunSummary run : result.runs) {
            System.out.printf(Locale.ROOT, "  repeat medians %-8s %s%n",
                run.mode.label, formatMedians(run.repeatMedians));
        }
    }

    private static String formatMedians(double[] values) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                text.append('/');
            }
            text.append(String.format(Locale.ROOT, "%.3f", values[i]));
        }
        return text.toString();
    }

    private static void printSummary(List<ShapeResult> results, String label) {
        System.out.println("# summary: median ms over all rounds (build time excluded)");
        System.out.println("shape,nodes,true_cold_ms,clear_cache_ms,clear_minus_true_ms,clear_vs_true_pct");
        for (ShapeResult result : results) {
            RunSummary trueRun = result.trueColdOrNull();
            RunSummary clearRun = result.clearCacheOrNull();
            if (trueRun == null || clearRun == null) {
                continue;
            }
            double trueCold = trueRun.stats.median;
            double clear = clearRun.stats.median;
            System.out.printf(Locale.ROOT, "%s,%d,%.4f,%.4f,%+.4f,%+.2f%n",
                result.name, result.nodes, trueCold, clear, clear - trueCold,
                percent(trueCold, clear));
        }
        System.out.printf(Locale.ROOT, "label,%s%n", label);
    }

    private static void printCsv(List<ShapeResult> results, String label) {
        System.out.println("# csv");
        System.out.println("label,shape,nodes,mode,median_ms,min_ms,max_ms,spread_ms,mean_ms,stddev_ms,"
                           + "prep_median_ms,repeat_medians_ms");
        for (ShapeResult result : results) {
            for (RunSummary run : result.runs) {
                Stats stats = run.stats;
                System.out.printf(Locale.ROOT, "%s,%s,%d,%s,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%.6f,%s%n",
                    label, result.name, result.nodes, run.mode.label,
                    stats.median, stats.min, stats.max, stats.spread(),
                    stats.mean, stats.stddev, run.prepMedian, formatMedians(run.repeatMedians));
            }
        }
    }

    /** Percent change from {@code baseline} to {@code candidate}; positive means candidate larger. */
    private static double percent(double baseline, double candidate) {
        return baseline == 0.0 ? Double.NaN : (candidate - baseline) / baseline * 100.0;
    }

    private static void printUsage() {
        System.out.println("""
            usage: ColdLayoutProbe [key=value ...]

              rounds=N    measured rounds per block (default 20)
              largeRounds=N  rounds for the 100k and superDeep shapes (default 10)
              warmup=N    minimum untimed warmup rounds per mode (default 2)
              warmupMs=N  untimed warmup wall-clock budget per shape (default 1500)
              repeats=N   independent blocks per shape (default 2)
              only=a,b    comma-separated shape names; default all
              mode=all    all | true | clear (default all)
              gc=true     run System.gc() between rounds, outside the timer (default true)
              label=text  tag written to the report and CSV (default current)
              help        print this help

            shapes: form-20k, dashboard-16, measure-2000, flat-flex-100k,
                    flat-grid-100k, superDeepGrid-1000, deepGrid-3_4, wideGrid-316
            """);
    }

    private record Options(int rounds, int largeRounds, int warmup, int warmupMillis, int repeats,
                           List<String> only, String mode, boolean gc, String label, boolean help) {

        int roundsFor(Shape shape) {
            return shape.large ? largeRounds : rounds;
        }

        static Options parse(String[] args) {
            int rounds = DEFAULT_ROUNDS;
            int largeRounds = DEFAULT_LARGE_ROUNDS;
            int warmup = DEFAULT_WARMUP;
            int warmupMillis = DEFAULT_WARMUP_MILLIS;
            int repeats = DEFAULT_REPEATS;
            List<String> only = List.of();
            String mode = "all";
            boolean gc = true;
            String label = "current";
            boolean help = false;

            for (String arg : args) {
                if (arg.equals("help") || arg.equals("--help") || arg.equals("-h")) {
                    help = true;
                    continue;
                }
                int split = arg.indexOf('=');
                if (split <= 0) {
                    throw new IllegalArgumentException("expected key=value, got: " + arg);
                }
                String key = arg.substring(0, split);
                String value = arg.substring(split + 1);
                switch (key) {
                    case "rounds" -> rounds = positive(key, value);
                    case "largeRounds" -> largeRounds = positive(key, value);
                    case "warmup" -> warmup = nonNegative(key, value);
                    case "warmupMs" -> warmupMillis = nonNegative(key, value);
                    case "repeats" -> repeats = positive(key, value);
                    case "only" -> only = Arrays.stream(value.split(","))
                        .map(String::trim)
                        .filter(entry -> !entry.isEmpty())
                        .toList();
                    case "mode" -> {
                        if (!value.equals("all") && !value.equals("true") && !value.equals("clear")) {
                            throw new IllegalArgumentException("mode must be all, true or clear");
                        }
                        mode = value;
                    }
                    case "gc" -> gc = Boolean.parseBoolean(value);
                    case "label" -> label = value;
                    default -> throw new IllegalArgumentException("unknown option: " + key);
                }
            }
            return new Options(rounds, largeRounds, warmup, warmupMillis, repeats, only, mode, gc, label, help);
        }

        boolean includes(String name) {
            return only.isEmpty() || only.contains(name);
        }

        private static int positive(String key, String value) {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(key + " must be > 0");
            }
            return parsed;
        }

        private static int nonNegative(String key, String value) {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(key + " must be >= 0");
            }
            return parsed;
        }
    }
}
