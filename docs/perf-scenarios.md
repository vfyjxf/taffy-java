# Phase and scenario benchmarks (JMH)

This document describes the **phase benchmarks**: a companion suite to
[`perf-benchmarks.md`](perf-benchmarks.md) that splits a layout run into the stages where a
difference against Rust taffy or jaffy can come from — style writes, tree construction, measure
callbacks, layout algorithms, style resolution, cache lookups/invalidation, direction resolution
and rounding.

The suite is diagnostic, not a gate. It records **what is measured and how to reproduce it**;
numbers belong in the run's JSON and in a separate report. Do not read a single score from this
suite as a conclusion about the Rust/jaffy gap: every class isolates one stage, and only the
comparison of the same class/parameter pair between two checkout revisions (or two libraries'
equivalent shapes) is meaningful.

## Phase map

| Phase | Where it lives in this port | Isolated by |
|---|---|---|
| Tree construction | `TaffyTree.newLeaf`, `newLeafWithMeasure`, `newWithChildren` | `LeafFlowBenchmark.newLeafWithMeasureOnly`, `buildTreeOnly`, `buildTreeAndLayout` |
| Style write + invalidation | `TaffyTree.setStyle`: store reference, bump direction epoch, `markDirty` ancestors | `StylePhaseBenchmark.setStyle*`, `styleChangeRelayout` |
| Style resolution + intrinsic sizing | `TaffyStyle` values resolved by `Resolve`/layout algorithms (no separate style tree) | `PhaseLayoutBenchmark.flatSizing/deepSizing` (`auto`, `percent`, `margin`) |
| Layout algorithm | `FlexboxComputer`, `BlockComputer`, `GridComputer` | `PhaseLayoutBenchmark.flatFixed/deepFixed` |
| Measure callback | `LayoutComputer.measureLeaf` → `MeasureFunc` | `LeafFlowBenchmark.layoutCold/layoutWarmChildren/measureDirect`, `IncrementalPhaseBenchmark.measure*` |
| Layout cache | `LayoutCache.get/store`, key comparison, measure-entry scan | `IncrementalPhaseBenchmark.repeatCompute/resizeRoot/styleChangeLeaf/addRemoveChild`, `InternalPhaseBenchmark.cache*` |
| Cache invalidation walk | `TaffyTree.clearCache` / `markDirty` per node | `IncrementalPhaseBenchmark.clearCaches` |
| Direction resolution | `LayoutComputer.resolveDirection` (memoized by tree epoch) | `InternalPhaseBenchmark.resolveDirectionWarm/resolveDirectionCold` |
| Rounding pass | `TaffyTree.roundLayout` (iterative, visits every node) | `PhaseLayoutBenchmark.*Rounded`, `InternalPhaseBenchmark.roundLayout`, `IncrementalPhaseBenchmark.geometryUnchangedRounded` |
| Allocation | all of the above | any benchmark with `-prof gc` (see below) |

## Suite map

All classes live under `src/jmh/java`. `BenchSupport` is the shared helper; the new
`PhaseSupport` owns the phase-tree builders and the fixed/text measure callbacks.

### `PhaseLayoutBenchmark` — 8 methods, 144 parameter combinations

No measure callbacks. Leaves have definite sizes for the `fixed` variant.

| Method | Shape | Parameters | Rounding | Isolates |
|---|---|---|---|---|
| `flatFixed` | one container + `nodeCount - 1` leaves | `display` = flex/block/grid; `nodeCount` = 1k/10k/100k | off | flat layout algorithm |
| `flatFixedRounded` | same tree | same | **on** | flat layout + rounding pass |
| `flatSizing` | same shape, `sizing` variant | display; nodeCount; `sizing` = auto/percent/margin | off | style resolution + intrinsic content sizing |
| `flatSizingRounded` | same | same | **on** | sizing + rounding |
| `deepFixed` | chain of `depth` containers, each with a continuing child + one leaf (`2 * depth + 2` nodes) | display; `depth` = 64/256/1024 | off | deep/recursive layout algorithm |
| `deepFixedRounded` | same | same | **on** | deep layout + rounding |
| `deepSizing` | same chain, `sizing` variant | display; depth; sizing | off | deep style resolution + content sizing |
| `deepSizingRounded` | same | same | **on** | deep sizing + rounding |

Sizing variants:

* `auto`: auto-sized leaf with 2px padding + 1px border, so the leaf must resolve its content box;
* `percent`: 10% x 10% leaf under a definite 2000x2000 container (the chain containers are 100% x
  100%), so percentage resolution is exercised instead of reading definite sizes;
* `margin`: fixed 10x10 leaf with a 2px margin on all sides.

### `LeafFlowBenchmark` — 7 methods, 26 parameter combinations

A column flex root of `leafCount` measured leaves (percentage width, auto height), so the measure
callback is reached for every leaf.

| Method | Tree lifetime | Parameters | Isolates |
|---|---|---|---|
| `newLeafWithMeasureOnly` | fresh per invocation | leafCount = 1k/10k; `measureKind` = fixed/text | leaf allocation + measure attachment, no parent, no layout |
| `buildTreeOnly` | fresh per invocation | same | leaves + root construction, no layout |
| `buildTreeAndLayout` | fresh per invocation | same | construction + one layout |
| `layoutCold` | trial-built, all caches cleared per invocation | same | layout + measure callbacks |
| `layoutWarmChildren` | trial-built, only root cache cleared | same | root layout while measured children answer from caches |
| `measureDirect` | callback only | same | the measure callback body |
| `layoutNoMeasure` | trial-built | leafCount = 1k/10k | the same layout with fixed-size leaves and no callback |

The build methods are the only methods that construct a tree per invocation; every other layout
benchmark follows the suite rule of one trial-built tree plus cache invalidation. `fixed` returns
a constant 10x10 content size; `text` uses the deterministic 16-word wrapping callback from
`BenchSupport`.

### `IncrementalPhaseBenchmark` — 9 methods, 29 parameter combinations

A flat column flex root with `nodeCount - 1` fixed 10x10 leaves, built and laid out once in
`@Setup(Level.Trial)`. Rounding is disabled for the incremental methods, mirroring upstream
`relayout.rs`, because the rounding pass visits every node on every layout.

| Method | Parameters | Semantics |
|---|---|---|
| `repeatCompute` | nodeCount = 1k/10k/100k | identical inputs again; a pure root-cache hit (sentinel, not layout throughput) |
| `resizeRoot` | same | available width alternates 2000/2500; root recomputes, children hit caches |
| `styleChangeLeaf` | same | one leaf height toggles 10/20; ancestors recompute |
| `addRemoveChild` | same | attach a detached leaf, lay out, detach it again |
| `measureCold` | leafCount 1k/10k/100k; measureKind fixed/text | all caches cleared per invocation |
| `measureWarmChildren` | same | only the root cache cleared |
| `clearCaches` | nodeCount 1k/10k/100k | only the per-node invalidation walk, no layout |
| `geometryUnchanged` | default shape (1111 nodes) | upstream `geometry_unchanged` shape (fanout 10, depth 3, containers cycling flex/block/grid); root cache hit, **rounding off** |
| `geometryUnchangedRounded` | same shape | same root cache hit, **rounding on**: the score is the fast path **plus** the tree-wide rounding pass |

The last pair is the direct measurement of what rounding adds when the layout itself is a cache
hit. `geometryUnchanged` is sub-microsecond and is a sentinel for the root fast path, not a layout
number; subtract/compare it with `geometryUnchangedRounded` only for the same checkout, JVM and
run.

### `StylePhaseBenchmark` — 7 methods, 21 parameter combinations

A flat column flex root with `nodeCount - 1` fixed leaves, built and laid out once.

| Method | Parameters | Semantics |
|---|---|---|
| `setStyleSameValue` | nodeCount = 1k/10k/100k | 10,000 writes of the same style instance to one leaf, no layout; normalized per write |
| `setStyleDifferentValue` | same | 10,000 writes alternating two style instances with different heights; normalized per write |
| `setStyleAcrossLeaves` | same | 10,000 writes spread over distinct leaves (pre-computed target list); normalized per write |
| `setStyleSameValueThenLayout` | same | the 10,000-write batch + one layout; score is for the whole batch |
| `setStyleDifferentValueThenLayout` | same | alternating-write batch + one layout |
| `styleChangeRelayout` | same | one `setStyle` + one layout (incremental relayout shape) |
| `computeAfterMarkDirty` | same | one `markDirty` + one layout, no style write |

This port has no style parser: `setStyle` stores the reference, bumps the direction epoch and
marks the node/ancestor caches dirty. The class prices that path; `computeAfterMarkDirty` is the
no-style-write control for `styleChangeRelayout`.

### `InternalPhaseBenchmark` — 7 methods, 13 parameter combinations

Lives in `dev.vfyjxf.taffy.tree` so it can use the package-private direction epoch bump as well
as the public cache/layout APIs.

| Method | Parameters | Semantics |
|---|---|---|
| `cacheGetMeasureHitX9` | — | look up each of the nine measure entries of a full `LayoutCache` once; normalized per lookup |
| `cacheGetMeasureMiss` | — | one lookup that compares all nine measure entries and misses |
| `cacheGetLayoutHit` | — | one `PERFORM_LAYOUT` hit |
| `cacheStoreX10` | — | fill an empty cache with one final-layout + nine measure entries (allocates a fresh cache per invocation, like upstream) |
| `resolveDirectionWarm` | depth = 64/256/1024 | 1,000 repeated direction queries on a node with a current memo stamp; normalized per query |
| `resolveDirectionCold` | same | bump the tree direction epoch, then resolve the deepest leaf of an INHERIT chain |
| `roundLayout` | nodeCount = 1k/10k/100k | one tree-wide rounding pass on an already laid-out flat tree |

These map one-to-one to the upstream `relayout.rs` cache micro-benchmarks
(`cache/get_measure_hit_x9`, `cache/get_measure_miss`, `cache/get_layout_hit`,
`cache/store_x10`). Direction and rounding have no upstream micro-benchmark; they exist because
this port added a memoized direction resolution and has a distinct rounding pass.

## Shapes and node counts

| Shape | Total nodes | Notes |
|---|---|---|
| flat | `nodeCount` | one container + `nodeCount - 1` leaves |
| deep | `2 * depth + 2` | `depth` containers + `depth` side leaves + bottom leaf + root wrapper |
| measured | `leafCount + 1` | measured leaves + column flex root |
| geometry | 1111 | fanout 10, depth 3, containers cycling flex/block/grid |

All trees are deterministic: no RNG is used by the phase suite (unlike `FlexboxBenchmark`'s random
styles), so a baseline and a candidate checkout build structurally identical trees. Style objects
are shared between leaves where they are never mutated; benchmarks that mutate a style own the two
instances they alternate between.

## Correspondence with Rust taffy and jaffy

Upstream Rust files are under `taffy/benches/benches/`; jaffy's JMH sources are
`src/jmh/java/net/morphismmc/jaffy/bench/` in the jaffy checkout (read-only reference for shape
names — no jaffy code is copied into this repository).

| Phase benchmark | Rust taffy benchmark | jaffy benchmark |
|---|---|---|
| `PhaseLayoutBenchmark.flatFixed` (flex) | `flexbox.rs` `wide` / flat hierarchy (style generator replaced by fixed leaves) | `FlexboxBench` `wide` |
| `PhaseLayoutBenchmark.flatFixed` (grid) | `grid.rs` `grid/wide` (a single container here, not NxN tracks) | `GridBench` wide grid |
| `PhaseLayoutBenchmark.deepFixed` | `flexbox.rs` deep / super-deep; `grid.rs` `grid/deep`, `grid/superdeep` | `FlexboxBench` `deepAuto`/`deepRandom`/`superDeep`; `GridBench` deep |
| `PhaseLayoutBenchmark.*Sizing` | no direct upstream benchmark; closest are the random `auto`/`percent` dimensions in `flexbox.rs` | `FlexboxBench` `deepRandom` (random dimensions) |
| `LeafFlowBenchmark.layoutCold/layoutWarmChildren` | no upstream relayout measure bench; upstream measures through `mixed.rs` parley text | `MixedBench` (measured text leaves), `NodeContextBench` |
| `LeafFlowBenchmark.build*` | `tree_creation.rs` (tree creation) | `JaffyTreeBuilder`/bench-tree setup (not measured separately) |
| `LeafFlowBenchmark.measureDirect` | `mixed.rs` measure callback | `MixedBench` `ParleyTextContext.measure`, `NodeContextBench` |
| `IncrementalPhaseBenchmark.repeatCompute` | `relayout.rs` comment "caches all still valid" | `RelayoutBench` (root cache path) |
| `IncrementalPhaseBenchmark.resizeRoot` | `relayout.rs` `relayout/resize` | `RelayoutBench.jaffyResize/vfyjxfResize` |
| `IncrementalPhaseBenchmark.styleChangeLeaf` | `relayout.rs` `relayout/{wide,balanced,deep}` (our tree is flat) | `RelayoutBench.jaffyRelayout/vfyjxfRelayout` |
| `IncrementalPhaseBenchmark.addRemoveChild` | no direct upstream benchmark | `RelayoutBench` tree lifecycle variants |
| `IncrementalPhaseBenchmark.measureCold/measureWarmChildren` | no direct upstream relayout benchmark; cache behavior follows `relayout.rs` | `MixedBench` / `NodeContextBench` (measured leaves, not cold/warm split) |
| `IncrementalPhaseBenchmark.geometryUnchanged` | `relayout.rs` `relayout/resize` comment / root cache path | `RelayoutBench` root path |
| `IncrementalPhaseBenchmark.geometryUnchangedRounded` | **no upstream counterpart** | **no jaffy counterpart** |
| `StylePhaseBenchmark.*` | no upstream benchmark (Rust `set_style` is a cheap field write + dirty walk) | no jaffy benchmark |
| `InternalPhaseBenchmark.cache*` | `relayout.rs` `cache/*` group (exact) | no jaffy cache micro-benchmark |
| `InternalPhaseBenchmark.resolveDirection*` | no upstream micro-benchmark | no jaffy micro-benchmark |
| `InternalPhaseBenchmark.roundLayout` | no upstream micro-benchmark; upstream rounds in `round_layout` | jaffy reports rounding as a hotspot but has no isolated benchmark |

The important non-matches:

* jaffy's `RelayoutBench` has only `relayout/{shape}` and `relayout/resize`; it has no cache
  micro-benchmarks and no rounding-on/off pair. Use `InternalPhaseBenchmark` for those.
* upstream `relayout.rs` disables rounding; this suite keeps that for the incremental methods and
  adds the `geometryUnchangedRounded` pair specifically to price it.
* the flat grid scenario is one container with 64 explicit auto columns, not upstream's NxN track
  grid; use `GridCoverageBenchmark`/`GridBenchmark` for track-sizing coverage.

## Running

```bash
# Compile benchmark sources only (no measurement)
./gradlew jmhClasses

# Phase classes only, default smoke profile (1 warmup + 1 measurement, 1 fork)
./gradlew jmhPhase

# One class or one method (includes/excludes are Java regexes over the FQN)
./gradlew jmhPhase -PjmhPhaseIncludes='.*InternalPhaseBenchmark.*'
./gradlew jmhPhase -PjmhPhaseIncludes='.*PhaseLayoutBenchmark.flatFixed$'

# Stable gate profile for one class
./gradlew jmhPhase \
  -PjmhPhaseIncludes='.*IncrementalPhaseBenchmark.*' \
  -PjmhPhaseWarmupIterations=3 -PjmhPhaseIterations=5 -PjmhPhaseFork=2
```

`jmhPhase` writes `build/reports/jmh/results-phase.json` and
`build/reports/jmh/output-phase.txt`. The default `jmh` task still discovers these classes
(they all match `.*Benchmark.*` and none matches `.*ExtendedBenchmark.*`), so a full
`./gradlew jmh` run includes them together with the pre-existing suite.

The Gradle task runs every parameter combination of the selected methods. To pin one combination,
use the generated JMH jar:

```bash
./gradlew jmhJar
java -Xss16m -jar build/libs/taffy-java-1.1.4-jmh.jar -l | grep Phase
java -Xss16m -jar build/libs/taffy-java-1.1.4-jmh.jar \
  'IncrementalPhaseBenchmark.styleChangeLeaf' \
  -p nodeCount=10000 -wi 3 -i 5 -f 2 -w 1s -r 1s \
  -rf json -rff /tmp/perf/style-change-10k.json
```

The `jmhPhase` task properties:

| Property | Default | Meaning |
|---|---|---|
| `jmhPhaseIncludes` | `.*(PhaseLayout|LeafFlow|IncrementalPhase|StylePhase|InternalPhase)Benchmark.*` | include regex (FQN) |
| `jmhPhaseWarmupIterations` | `1` | warmup iterations |
| `jmhPhaseIterations` | `1` | measurement iterations |
| `jmhPhaseFork` | `1` | forks |

Use at least `-PjmhPhaseWarmupIterations=3 -PjmhPhaseIterations=5 -PjmhPhaseFork=2` for a
decision; the 1/1 default only proves the benchmark runs.

## Allocation profiling with `-prof gc`

Timing tells `which` phase is hot; `-prof gc` tells whether that phase allocates per node. Run the
same single benchmark/parameter tuple, with and without the change, and compare the normalized
allocation rate:

```bash
./gradlew jmhJar
java -Xss16m -jar build/libs/taffy-java-1.1.4-jmh.jar \
  'PhaseLayoutBenchmark.flatFixedRounded' \
  -p display=flex -p nodeCount=10000 \
  -wi 3 -i 5 -f 1 -w 1s -r 1s \
  -prof gc -rf json -rff /tmp/perf/flat-fixed-rounded-10k-gc.json
```

`-prof gc` adds these secondary metrics to the JSON (and to the console table for the fork):

| Metric | Meaning |
|---|---|
| `gc.alloc.rate.norm` | bytes allocated per operation (normalized by `@OperationsPerInvocation`) |
| `gc.alloc.rate` | MB/sec allocated |
| `gc.count` | GCs during the measurement |
| `gc.time` | total GC pause in ms |

Recommended pairs and what a change in their allocation rate means:

| Pair | Read the difference as |
|---|---|
| `layoutNoMeasure` vs `layoutCold` | allocation and time added by the measure callback path (the same tree shape, with/without callbacks) |
| `measureDirect` vs `layoutCold` | callback body vs the layout plumbing that calls it |
| `flatFixed` vs `flatFixedRounded` | per-node allocation of the rounding pass (`Layout`, `FloatPoint`, `FloatSize`, scrollable overflow) |
| `geometryUnchanged` vs `geometryUnchangedRounded` | rounding allocation on the root-cache-hit path (1111 nodes) |
| `cacheGetMeasureHitX9` vs `cacheGetMeasureMiss` | cache lookup work/allocation on hit vs full miss scan |
| `cacheStoreX10` | entry reuse: after the first fill, stores should not allocate new entries |
| `buildTreeOnly` vs `newLeafWithMeasureOnly` | root/children construction vs leaf construction |
| `setStyleSameValue` vs `setStyleDifferentValue` | any value-dependent work in `setStyle` (there is none in this port) |

Caveats:

* `-prof gc` instruments allocation, not just GC; do not compare its timing scores with a
  non-`gc` run. Use it for `gc.alloc.rate.norm`, not for throughput gates.
* The first fill of an empty `LayoutCache` allocates entries; later stores reuse them. Measure a
  store loop that starts from a populated cache when the question is steady-state allocation.
* For 100k and 1024-deep cases, run with `-f 1` and a fixed `-p` tuple; the fork's own setup
  creates the tree, which is not part of the measured operation.
* `gc.alloc.rate.norm` is per `@OperationsPerInvocation`. For example
  `setStyleSameValue` is normalized to one of its 10,000 writes, while
  `setStyleSameValueThenLayout` reports the whole batch plus one layout.

## Cost and parameter budget

The phase suite contains 38 `@Benchmark` methods and 233 parameter combinations:

| Class | Methods | Combinations |
|---|---:|---:|
| `PhaseLayoutBenchmark` | 8 | 144 |
| `LeafFlowBenchmark` | 7 | 26 |
| `IncrementalPhaseBenchmark` | 9 | 29 |
| `StylePhaseBenchmark` | 7 | 21 |
| `InternalPhaseBenchmark` | 7 | 13 |

The Gradle smoke profile runs each combination with one 1s warmup + one 1s measurement, so the
suite alone is at least ~8 minutes of wall time before trial setup; the 100k flat grid, 100k
measured-leaf layouts and 1024-deep chains are the expensive combinations. This is why
`jmhPhase` accepts an include regex: run one class or one method at a time when iterating, and
use the full matrix only for the final serial run. The default `jmh` task also runs the
pre-existing suite, so do not use its total wall time as the phase-suite budget.

## Verification status

* `./gradlew jmhClasses` passes (all phase classes and the annotation processor).
* `java -Xss16m -jar build/libs/taffy-java-1.1.4-jmh.jar -l` discovers all 38 phase benchmark ids,
  including `geometryUnchanged` and `geometryUnchangedRounded`.
* No benchmark scores are recorded in this document. The authoritative phase run is a serial run
  (no concurrent JMH forks) using the commands above; keep the JSON next to the report.
