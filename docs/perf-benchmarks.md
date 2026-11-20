# Performance benchmarks (JMH)

This document describes the JMH suite used as the performance gate for the upstream-alignment
work. It covers flexbox, grid, block/float/BFC, mixed and realistic UI trees, incremental layout,
tree lifecycle and direction/out-of-flow scenarios.

The suite is intentionally split into a **default smoke profile** and an **extended profile**:

* the default profile runs every class that does not match `.*ExtendedBenchmark.*`; it uses
  small/medium inputs and is sized to finish in under ~10 minutes on a developer laptop;
* the extended profile holds the 100,000-node workloads (PanGUI `wide_no_wrap_simple_many` and
  `fit_nesting`, and the 100k tree-lifecycle variants) and is only run explicitly.

## Quick start

```bash
# Compile the benchmark sources only
./gradlew jmhClasses

# Default smoke run: all non-Extended benchmarks, 1 warmup + 1 measurement iteration
./gradlew jmh

# Large-input benchmarks only
./gradlew jmhExtended
```

The default JSON result lands in `build/reports/jmh/results.json`; the extended result lands in
`build/reports/jmh/results-extended.json`.

## Suite map

| Class | @Benchmark methods | Default parameters | Coverage | Upstream reference |
|---|---|---|---|---|
| `FlexScenarioBenchmark` | 6 | nodeCount 100 / 1,000 / 10,000; row+column; baseline 100/1,000; depth 100/1,000 | row/column, wrap/no-wrap, grow, shrink, baseline alignment, deep nesting | `benches/flexbox.rs` |
| `PanguiBenchmark` | 11 | 100-15,000 nodes per workload | `wide_no_wrap_simple_few` (1k/10k; 100k in Extended), `wide_wrapping`, `nested_vertical_stack`, equal/weighted expand, percentage+ratio, min/max constraints, perpendicular wrap, pixels+min-expand, padding/margin | `benches/pangui/*.txt` |
| `GridCoverageBenchmark` | 4 | cells 100 / 1,000 / 10,000; auto-flow 4 modes; nested `2_5`, `3_4` | fixed/fr/auto tracks, auto-flow row/column/dense, spanning items, nested grids | `benches/grid.rs` |
| `BlockFloatBenchmark` | 7 | depth/items 100 / 1,000 | deep block nesting, margin collapsing, left/right floats, `clear`, overflow scroll, `flow-root`, `contain: layout` | upstream float/BFC tests and block layout |
| `LdlibUiBenchmark` | 5 | panels 10/100/1,000; leaves 100/1k/10k; cards 24/96/384; scroll panels 10/100 | LDLib-style panel/label/button/scroll trees, measure-heavy leaves, percentage+aspect-ratio, grid of flex cards | `benches/mixed.rs` (measure-heavy shape) |
| `IncrementalLayoutBenchmark` | 7 | 4 container displays; 3 shapes (wide/balanced/deep); 200 measured leaves | cold layout, style-change relayout, geometry-unchanged cache hit, resize relayout, add/remove + relayout, measure cold vs warm children | `benches/relayout.rs` |
| `TreeLifecycleBenchmark` | 4 | 1,000 / 10,000 nodes | `newLeaf`, `newWithChildren`, subtree removal, `clear` throughput | `benches/tree_creation.rs` |
| `AdvancedLayoutBenchmark` | 5 | depth 100/500; childCount 100/1k/10k; cells 100/1k | alternating RTL/LTR nesting, RTL grid with per-item direction, absolute positioning with all inset combinations, grid+flex mixed, all-algorithm nesting | upstream RTL/abspos/mixed coverage |
| `PanguiExtendedBenchmark` | 2 | 100,000 nodes | `wide_no_wrap_simple_many`, `fit_nesting` (~101k nodes) | `benches/pangui/*.txt` |
| `TreeLifecycleExtendedBenchmark` | 4 | 100,000 nodes | 100k creation/removal/clear | `benches/tree_creation.rs` |

Totals: 10 new benchmark classes, 55 `@Benchmark` methods, 114 benchmark runs across all
parameter combinations (108 default + 6 extended).

`BenchSupport` is a shared, non-benchmark helper: deterministic tree builders, node collection,
cache invalidation and a word-wrapping measure function.

## Methodology

* **The tree is built once.** Every state builds its tree in `@Setup(Level.Trial)`; the measured
  method only calls `computeLayout` (plus `getLayout`/blackhole consumption).
* **Cold layout means "all caches invalid".** Layout benchmarks iterate every node in
  `@Setup(Level.Invocation)` and call `clearCache`, which is the Java equivalent of upstream's
  `TaffyLayoutTree::mark_all_dirty`. Invocation setup is not included in the measurement.
* **Incremental benchmarks keep caches valid.** `styleChangeRelayout` only changes one leaf via
  `setStyle` (which dirties the leaf and its ancestors); sibling caches remain valid.
  `measureWarmChildren` clears only the root cache, so the root re-runs while measured children
  are answered from their caches.
* **Rounding is disabled for incremental benchmarks**, mirroring upstream `relayout.rs`: the
  rounding pass visits every node on every layout and would otherwise hide the cache effects.
* **Determinism.** Trees use fixed shapes and a fixed seed (`12345`) where random styles are
  used, so a baseline and an aligned checkout build structurally identical trees.
* **Stack size.** The `jmh` and `jmhExtended` tasks pass `-Xss16m` for the 100/1,000-deep
  recursion benchmarks. Add the same option when launching the JMH jar directly.
* **Measurement modes.** Class defaults are `AverageTime` with microsecond or millisecond output;
  the Gradle smoke profile overrides warmup/measurement to 1 iteration each. Gate runs should
  override them (see below).

## Running a subset

The `jmh` task reads its options from Gradle properties, so a subset run does not need a new
task:

```bash
# Only the flex and grid classes (include/exclude are Java regexes matched against the FQN)
./gradlew jmh \
  -PjmhIncludes='.*(FlexScenarioBenchmark|GridCoverageBenchmark).*'

# One class, stable gate profile
./gradlew jmh \
  -PjmhIncludes='.*IncrementalLayoutBenchmark.*' \
  -PjmhWarmupIterations=3 -PjmhIterations=5 -PjmhFork=2 \
  -PjmhExcludes='.*ExtendedBenchmark.*'
```

| Property | Default | Meaning |
|---|---|---|
| `jmhIncludes` | `.*Benchmark.*` | include regex (FQN) |
| `jmhExcludes` | `.*ExtendedBenchmark.*` | exclude regex (FQN) |
| `jmhWarmupIterations` | `1` | warmup iterations (default is a smoke profile) |
| `jmhIterations` | `1` | measurement iterations |
| `jmhFork` | `1` | forks |

For decisions, use at least `-PjmhWarmupIterations=3 -PjmhIterations=5` and preferably
`-PjmhFork=2`. The 1/1 default is only meant to prove the benchmark runs and to catch crashes,
not to resolve small performance differences.

### Per-parameter runs and the raw JMH CLI

The Gradle task runs every parameter combination of the selected benchmarks. To pin one
combination, use the generated JMH jar:

```bash
./gradlew jmhJar
java -Xss16m -jar build/libs/taffy-java-1.1.4-jmh.jar -l | grep Benchmark
java -Xss16m -jar build/libs/taffy-java-1.1.4-jmh.jar \
  'GridCoverageBenchmark.trackSizing' \
  -p trackMode=fr -p cellCount=10000 \
  -wi 3 -i 5 -f 2 -w 1s -r 1s
```

Useful options: `-wi` warmup iterations, `-i` measurement iterations, `-f` forks, `-w`/`-r`
iteration time, `-p key=value` benchmark parameter, `-prof gc` for allocation/GC profiling,
`-l` to list benchmark ids without running them.

### Extended profile

```bash
# All 100k-node workloads
./gradlew jmhExtended

# A single extended workload with a shorter override
./gradlew jmhExtended \
  -PjmhExtendedIncludes='.*PanguiExtendedBenchmark.*' \
  -PjmhExtendedWarmupIterations=1 -PjmhExtendedIterations=1
```

`jmhExtended` accepts `jmhExtendedIncludes`, `jmhExtendedWarmupIterations` and
`jmhExtendedIterations`; everything else follows the same JMH semantics as `jmh`.

## Exporting results as JSON

The Gradle tasks already write JSON. With the raw jar, ask JMH for JSON explicitly:

```bash
java -Xss16m -jar build/libs/taffy-java-1.1.4-jmh.jar \
  '.*' -rf json -rff build/reports/jmh/my-run.json \
  -wi 3 -i 5 -f 2
```

Useful extra outputs: `-rf csv -rff results.csv` (CSV) and `-o output.txt` (human-readable log).
Keep the raw JSON as the artifact; it contains the parameters, JVM version, iteration counts and
per-iteration `rawData`, which are all needed to explain a difference.

Extract a flat table:

```bash
jq -r '.[] |
  [.benchmark,
   ((.params // {}) | to_entries | sort_by(.key) | map("\(.key)=\(.value)") | join(",")),
   .primaryMetric.score,
   .primaryMetric.scoreUnit] | @tsv' \
  build/reports/jmh/results.json | sort
```

## Comparing baseline and aligned runs

1. **Same host, same JDK, same Gradle.** Use two worktrees (for example the baseline commit and
   the aligned commit) rather than rebuilding the same worktree between measurements, and make
   sure both use the same JDK/Gradle version. The JMH JSON records the JVM version — check it.
2. **Build first.** Run `./gradlew jmhClasses jmhJar` in both worktrees before measuring, then
   do not compile while benchmarks run. Close CPU-heavy applications; keep the machine on AC
   power and avoid thermal throttling.
3. **Run identical configurations.** Same `-PjmhIncludes`, iterations, forks and JVM args. For a
   gate run use `-PjmhWarmupIterations=3 -PjmhIterations=5 -PjmhFork=2` (or higher).
4. **Name results after the commit.** Copy `results.json` to a stable location, e.g.
   `/tmp/perf/baseline-<sha>.json` and `/tmp/perf/aligned-<sha>.json`. Do not commit measurement
   JSON to the repository.
5. **Estimate the noise floor.** Run the same checkout twice and compare the two JSON files
   before interpreting a baseline-vs-aligned difference. A 1/1 smoke run is noisy; do not gate on
   small deltas from it.
6. **Compare per benchmark and parameter set** with the script below.

```python
#!/usr/bin/env python3
# usage: compare_jmh.py baseline.json aligned.json
import json
import sys

def load(path):
    result = {}
    with open(path, encoding="utf-8") as handle:
        for entry in json.load(handle):
            params = ",".join(f"{k}={v}" for k, v in sorted(entry.get("params", {}).items()))
            key = (entry["benchmark"], params)
            result[key] = (entry["primaryMetric"]["score"], entry["primaryMetric"]["scoreUnit"])
    return result

baseline = load(sys.argv[1])
aligned = load(sys.argv[2])
for key in sorted(baseline.keys() & aligned.keys()):
    base, base_unit = baseline[key]
    new, new_unit = aligned[key]
    assert base_unit == new_unit, (key, base_unit, new_unit)
    ratio = new / base if base else float("nan")
    print(f"{key[0]}\t{key[1]}\t{base:.3f}\t{new:.3f}\t{ratio:.2f}x\t{base_unit}")
```

`ratio > 1.0` means the aligned build is slower for that benchmark/parameter combination.
Investigate anything above roughly 5% in a 3/5/2 run, or above 10% in a 1/1 smoke run; re-run
the single benchmark with the raw jar (`-p` to pin the parameter) to confirm.

## Scenario notes and caveats

* `geometryUnchanged` intentionally measures a single root-cache hit (sub-microsecond). It is a
  sentinel for cache correctness/overhead, not a layout throughput number.
* `measureCold` and `measureWarmChildren` differ only in which caches are cleared; comparing the
  two isolates the measure-callback path from the layout cache.
* `styleChangeRelayout` uses upstream's wide (100x2), balanced (10x4) and deep (2x12) shapes,
  cycling flex/block/grid containers by depth; rounding is disabled.
* The float/`flow-root`/`contain: layout` benchmarks exercise the float/BFC code path. On
  revisions before the float port they still run (the style fields are accepted) but only
  measure the no-float path; do not compare across that boundary.
* Parameter scales were chosen for the smoke profile. Add new 100k-node cases to an
  `*ExtendedBenchmark` class rather than to a default class.
* The 1,000-deep cases rely on `-Xss16m`; the Gradle tasks set it, but a raw `java -jar` invocation
  must pass it too.
