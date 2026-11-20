# True-cold layout check (p23)

Date: 2026-11-21.  Checkout: `9704cb0` (p22 merged).  JDK 17.0.14 (JetBrains), macOS arm64,
10 CPUs.  Baseline: `~/.m2/repository/dev/vfyjxf/taffy/1.2.0-local5/taffy-1.2.0-local5-all.jar`.

This document answers one question: **when the whole tree is created for the first frame, how much
of the p19/p20/p22 improvement survives?** It also explains why the JMH suite's existing
"clearCache cold" convention cannot answer that question.

## 1. Why "clearCache cold" is not cold

The JMH suite builds one tree in `@Setup(Level.Trial)`, then calls
`TaffyTree.clearCache(node)` for every node in `@Setup(Level.Invocation)` and calls the following
`computeLayout` "cold layout". That mirrors upstream taffy's `mark_all_dirty`, but in this port it
does **not** reset every cache:

* `TaffyTree.clearCache` clears the node's `LayoutCache` **and** its block item list
  (`NodeData.clearBlockItemCache`, see `TaffyTree.java:1412-1420`).
* The p20 `FlexboxComputer.FlexItemCache` and the p22 `GridComputer.GridItemCache` are stored on
  `NodeData` and are only invalidated by a content/children version change. `clearCache` does not
  touch them. They therefore survive every invocation after the first one.
* The p19 same-BFC layout reuse is a marked `LayoutCache` entry, so it **is** cleared.

Consequence: the second and later JMH invocations reuse flex/grid item metadata built by the first
layout. The reported median is mostly a warm-item-list relayout, not the first-frame cost. A second
effect is subtler: keeping one tree alive and clearing its caches is itself different from building a
fresh tree. The local5 control (which has no p20/p22 caches) was often **slower** under clearCache
than under true cold (`old clear/true` = 1.29 for form, 1.27 for flat flex, 1.19 for wide grid).
Part of the apparent "gain" of the new build is therefore compensation for a benchmark that
penalizes the old build, not reusable layout work.

## 2. Probe and method

`ColdLayoutProbe` (`src/jmh/java/dev/vfyjxf/taffy/benchmark/ColdLayoutProbe.java`) is a plain Java
`main` class; it never starts JMH. Run it through the `coldLayoutProbe` JavaExec task:

```bash
./gradlew coldLayoutProbe
./gradlew coldLayoutProbe -PcoldProbeRounds=20 -PcoldProbeRepeats=2
./gradlew coldLayoutProbe -PcoldProbeOnly=wideGrid-316 -PcoldProbeMode=true
```

It measures two semantics on the same compiled code:

* **true-cold**: a new `TaffyTree`, new styles and new measure callbacks for every round. Tree
  construction is outside the timer; a full GC runs before the timer so the previous round's tree
  and the build temporaries are collected. Only `computeLayout` is timed.
* **clear-cache**: one tree is built and laid out once; every round clears every node's cache
  before the timed `computeLayout`. This is the existing JMH convention.

Default runs: 20 rounds for the small shapes, 10 for the 100k-node and superDeep shapes (one round
costs 50-500 ms there), two repeat blocks per shape, median + min/max + spread. The
`prep_med_ms` column is the tree build for true-cold and the clearCache walk for clear-cache;
both are outside the timer. The old baseline is measured by running the **same compiled probe and
benchmark classes** with only the main jar replaced:

```bash
./gradlew coldLayoutProbe \
  -PcoldProbeOldJar="$HOME/.m2/repository/dev/vfyjxf/taffy/1.2.0-local5/taffy-1.2.0-local5-all.jar" \
  -PcoldProbeLabel=local5
```

The probe prints the `taffy classes` code source, so a run cannot silently use the wrong jar.

The host had a sustained external load (load average 6-15) during this check. Standalone medians
drifted by 2-3x between runs. The numbers below therefore come from a **paired two-process
harness**: one JVM per classpath, the idle JVM paused with SIGSTOP, the active JVM resumed for one
round, current and old alternated every round, first five rounds discarded, both wall time and
per-thread CPU time recorded. The harness was temporary and is not committed; the permanent
`ColdLayoutProbe` reproduces the standalone version of the same two modes. Absolute milliseconds
below are inflated by the load and the smaller paired-run heap (`-Xms1g -Xmx2g`); compare values
inside a row, not across rows or with older reports.

The eight shapes are the benchmark suite's own builders:

| shape | nodes | builder |
|---|---:|---|
| form-20k | 20,055 | `Ldlib2UiBenchmark.buildForm(20000)` |
| dashboard-16 | 1,029 | `Ldlib2UiBenchmark.buildDashboard(16)` |
| measure-2000 | 2,001 | `Ldlib2UiBenchmark.buildMeasureTree(2000)` |
| flat-flex-100k | 100,000 | `PhaseSupport.buildFlat(flex, 100000, fixed)`, rounding off |
| flat-grid-100k | 100,000 | `PhaseSupport.buildFlat(grid, 100000, fixed)`, rounding off |
| superDeepGrid-1000 | 1,001 | `GridBenchmark` grid/superdeep, rounding on |
| deepGrid-3_4 | 7,381 | `GridBenchmark` grid/deep 3_4, rounding on |
| wideGrid-316 | 99,857 | `GridBenchmark` grid/wide 316, rounding on |

## 3. Results

### 3.1 Paired true-cold vs clear-cache

One representative paired run per row. `old` is local5, `new` is this checkout. `Δ` is the median
of the per-round current/old ratios, not the difference of the rounded ms columns.

| shape | nodes | old true ms | new true ms | Δ true | old clear ms | new clear ms | Δ clear | old clear/true | new clear/true |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| form-20k | 20,055 | 30.88 | 28.17 | **-11.1%** | 39.73 | 27.04 | -35.6% | 1.29 | 0.96 |
| dashboard-16 | 1,029 | 3.60 | 3.34 | **-6.5%** | 3.86 | 3.24 | -18.6% | 1.07 | 0.97 |
| measure-2000 | 2,001 | 2.63 | 2.35 | **-12.8%** | 2.91 | 2.27 | -21.7% | 1.11 | 0.97 |
| flat-flex-100k | 100,000 | 144.99 | 128.69 | **-15.3%** | 183.77 | 132.69 | -30.6% | 1.27 | 1.03 |
| flat-grid-100k | 100,000 | 178.22 | 144.49 | **-13.3%** | 178.61 | 151.82 | -21.4% | 1.00 | 1.05 |
| superDeepGrid-1000 | 1,001 | 11.56 | 11.18 | **-4.4%** | 11.86 | 10.85 | -8.6% | 1.03 | 0.97 |
| deepGrid-3_4 | 7,381 | 186.79 | 186.62 | +2.6% | 183.13 | 163.86 | -15.8% | 0.98 | 0.88 |
| wideGrid-316 | 99,857 | 507.52 | 565.57 | +16.5% | 604.15 | 459.31 | -40.1% | 1.19 | 0.81 |

Notes on the noisy rows:

* **form-20k**: two paired runs gave -11.1% and -14.0% true-cold. The earlier standalone pair
  that suggested 0% was not interleaved and was taken during a load shift; it did not reproduce.
* **dashboard-16 / measure-2000**: a second clean paired run gave -4.3% and -12.6% true-cold
  (CPU-time), consistent with this table.
* **flat-flex-100k**: paired runs gave -15.3% and -16.7%; standalone runs suggested up to -30%.
  One load-hit run showed +23% and is discarded as an outlier. Treat the true-cold gain as
  **-15% to -30%**, with -15% the conservative paired number.
* **superDeepGrid-1000**: -4.4% and -6.8% true-cold in paired runs, but the minimum-time ratio
  ranged 0.93-1.02 and one standalone run was 6-16% slower. There is **no reliable true-cold
  gain** here; p22's grid cache buys nothing on a 1x1 chain and may add a small store/validate
  overhead.
* **deepGrid-3_4**: wall time +2.6%, CPU time -9.7%, minimum-time ratio 0.85. The sign is not
  resolved; call it **0 to -10%**.
* **wideGrid-316**: wall time +16.5%, CPU time +0.3%, minimum-time ratio 0.92. The wall number is
  load noise. True cold is **flat**; the -28% to -40% clear-cache gain is p22's cross-pass grid
  item cache, which a fresh tree never exercises.

### 3.2 What this means for the existing "cold" numbers

The reported clearCache-cold numbers came from other hosts and earlier checkouts, so they are not
directly comparable in absolute ms. What is comparable is the delta:

| shape | previously reported "cold" | clear-cache delta measured here | true-cold delta measured here | verdict |
|---|---:|---:|---:|---|
| form-20k | 9.79 ms (p20 report) | -34% to -36% | -11% to -14% | clear-cache overstates by ~2-3x |
| dashboard-16 | 1.042 ms (p20 report) | -19% | -4% to -7% | overstates by ~3x |
| measure-2000 | 1.11 ms (p20 report) | -22% | -13% | overstates by ~1.7x |
| flat-flex-100k | 68.3 ms (p15 report) | -31% to -35% (one load-hit run -46%) | -15% to -30% | smaller, but still a real gain |
| flat-grid-100k | 83.6 ms (p15 report) | -21% to -27% | -13% to -16% | overstates by ~1.5x |
| superDeepGrid-1000 | 47.6 ms (p15 report) | -9% | -4% to -7%, noise | no reliable gain |
| deepGrid-3_4 | — | -16% | 0 to -10% | uncertain |
| wideGrid-316 | 206.8 ms (p15 report) | -28% to -40% | ~0% | **artifact: cross-pass cache only** |

The two headline cases are **wideGrid-316** and **superDeepGrid-1000**:

* wideGrid: p22's cross-pass `GridItemCache` makes the clear-cache relayout 28-40% faster, but a
  fresh 316x316 grid is not faster at all. The win belongs to incremental relayout, not to cold
  layout.
* superDeep: the clear-cache delta is around -9%, but true cold is flat or slightly worse. The
  1x1 chain has one item per container and cannot reuse a meaningful item list.

The p20 flex cache is more useful on a first layout because a container can be laid out several
times within one pass (intrinsic sizing, then final layout): form and flat flex keep a real
true-cold gain. It is still much smaller than the clear-cache number.

### 3.3 Cache-reuse share

`new clear/true` in the table is the fraction of the true-cold time that the clear-cache path
still spends: values below 1.00 mean the surviving flex/grid item caches help; values around 1.00
mean they do not.

| shape | new clear/true | old clear/true | clear-cache inflation of the delta |
|---|---:|---:|---:|
| form-20k | 0.96 | 1.29 | ~+25 points |
| dashboard-16 | 0.97 | 1.07 | ~+12 points |
| measure-2000 | 0.97 | 1.11 | ~+9 points |
| flat-flex-100k | 1.03 | 1.27 | ~+15 points |
| flat-grid-100k | 1.05 | 1.00 | ~+8 points |
| superDeepGrid-1000 | 0.97 | 1.03 | ~+4 points |
| deepGrid-3_4 | 0.88 | 0.98 | ~+6 to +18 points |
| wideGrid-316 | 0.81 | 1.19 | **~+36 points** |

The `old clear/true` column is the control. A value above 1.00 means "clearCache is slower than
building a fresh tree even though there are no item caches to reuse" - the legacy benchmark is not
a neutral cold baseline. A value below 1.00 for the new build (`wideGrid` 0.81, `deepGrid` 0.88)
means the surviving item cache is doing real work, but only on the reused-tree path.

## 4. Noise and limitations

* **Host load.** Load average 6-15 during the run; standalone medians moved 2-3x. The paired
  harness reduces this but cannot remove CPU-frequency and memory-bandwidth effects. The absolute
  ms above are from that harness and are 1.5-2x the values an idle machine produces; use the
  within-row deltas.
* **CPU time vs wall time.** Where both were recorded (dashboard, measure, flat grid), per-thread
  CPU time agreed with wall time. It disagreed on wideGrid (+0.3% CPU vs +16.5% wall) and deepGrid
  (-9.7% CPU vs +2.6% wall). The table reports wall time; the notes give the CPU number for those
  two rows.
* **JIT warm-up.** Tiered compilation needs hundreds of calls for small shapes; a fixed small
  warm-up made `measure-2000` look several times slower. The probe warms by wall-clock budget (default
  1.5 s, 3-5 s in the paired runs) plus a minimum round count. Even so, first-round outliers are
  visible; the first five rounds are discarded in the paired analysis.
* **GC.** A full GC runs outside the timer, so the measured operation starts from a comparable
  heap. GC still happens inside a 100k layout when it allocates; that is real first-frame cost.
  The paired harness used `-Xms1g -Xmx2g` (two JVMs) while `ColdLayoutProbe` uses
  `-Xms2g -Xmx3g`; the different heap changes allocation/GC pressure and contributes to the
  absolute difference.
* **Baseline scope.** local5 was published before p16/p17/p18 as well as p19/p20/p22. The
  true-cold delta is therefore an **upper bound on the p19/p20/p22 contribution**, not an isolated
  measurement. A per-commit jar series is required to attribute the gain precisely.
* **Shapes.** 8 shapes, one tree size each; no allocation profiling, no JFR, no JMH. 10/20 rounds
  per block, two blocks. Treat differences under ~10% as unresolved on a loaded host.

## 5. Recommendations

1. **True cold must rebuild the tree.** If a benchmark builds one tree and calls `clearCache`,
   call it `layout-cache-cold` or `mark_all_dirty relayout`, not cold. The first-frame number is
   the tree-build-plus-first-layout number, with the build outside the timer.
2. **Or add a benchmark-only item-cache reset.** If rebuilding a 100k tree is too expensive for a
   JMH state, add a package-private `clearItemCaches(NodeId)` (or a tree-wide walk) that clears
   `flexItemCache` and `gridItemCache` as well as `blockItemCache`, and call it from the
   invocation setup. Expose it only in the benchmark/test surface; production has no reason to
   clear these caches.
3. **Keep `clearCache` for what it measures.** It is the right tool for "every node is dirty,
   but the tree and its derived item metadata are still valid" - the relayout/incremental case.
   p20/p22's real value is there, and the incremental benchmarks already capture it.
4. **Do not gate on a clearCache-cold delta alone.** For wideGrid and superDeep it can show a
   double-digit gain while true cold is flat. Require the true-cold pair before claiming a
   cold-layout win.
5. **For comparisons on a shared host, use paired runs.** Alternate current and old at round or
   short-session granularity, report median + min + spread, and discard the first rounds. A
   single standalone JMH run on a loaded machine is not evidence for a <20% difference.
6. **Do not compare absolute numbers across hosts or across heap sizes.** The existing perf docs
   mix machines; use the delta within one paired run, and keep the raw log next to the report.

## 6. Reproduce

```bash
# Current build, both modes, all shapes (about 1-2 minutes on an idle machine)
./gradlew coldLayoutProbe

# One shape, true-cold only, 20/10 rounds per block
./gradlew coldLayoutProbe -PcoldProbeOnly=wideGrid-316 -PcoldProbeMode=true \
  -PcoldProbeRounds=20 -PcoldProbeLargeRounds=10 -PcoldProbeRepeats=2

# local5 baseline, same compiled probe and benchmark classes
./gradlew coldLayoutProbe \
  -PcoldProbeOldJar="$HOME/.m2/repository/dev/vfyjxf/taffy/1.2.0-local5/taffy-1.2.0-local5-all.jar" \
  -PcoldProbeLabel=local5

# Options: rounds, largeRounds, warmup, warmupMs, repeats, only, mode, gc, label
```

The probe prints the code source of `TaffyTree`, the node count, median/min/max/spread, per-block
medians, and a CSV block. Keep the CSV with the report; the doc's tables are derived from the
paired harness, not from a single CSV line.
