# Memory footprint (retained heap)

This document records the retained-heap cost of the benchmark tree shapes. It complements
[perf-benchmarks.md](perf-benchmarks.md) and [perf-scenarios.md](perf-scenarios.md), which answer
"how fast is a layout"; this one answers "how much memory does a built and laid-out tree keep
alive". It is not a JMH benchmark: `MemoryFootprintMain` builds one tree at a time, forces full
GCs and prints used-heap deltas. No benchmark score is recorded here.

## Quick start

```bash
# Compile the benchmark sources (the probe lives next to the JMH classes)
./gradlew jmhClasses

# Default: 5 rounds x 4 shapes, median + min/max spread
./gradlew memoryFootprint

# Fewer/more rounds, a single shape, a stable label for CSV rows
./gradlew memoryFootprint -PmemoryFootprintRounds=7 -PmemoryFootprintOnly=flat-flex-100k

# Optional worst case: 8 extra layouts at other viewport widths fill the per-node measure cache
./gradlew memoryFootprint -PmemoryFootprintFill=8 -PmemoryFootprintOnly=form-20k
```

The task is a `JavaExec` (not a `JMHTask`) with `-Xss512m -Xmx4g`; it prints a human summary
and a `# csv` block with columns
`label,scenario,nodes,stage,median_bytes,median_per_node,min_bytes,max_bytes`.

## Method

`MemoryFootprintMain` (`src/jmh/java/dev/vfyjxf/taffy/benchmark/MemoryFootprintMain.java`)
walks one tree life cycle per round and samples
`ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()` after three
`System.gc()` calls (+30 ms settle) at five points:

| Point | What is live | Report label |
|---|---|---|
| baseline | nothing of the tree; previous rounds already dropped and collected | `base` |
| after build | node records, styles, child lists, dense id arrays; all layout caches cold | `tree` |
| after layout | one cold `computeLayout` + one warm (tree-clean) call: layout records, cache entries, cross-pass item lists | `layout` |
| after cache clear | every node's `clearCache` (the suite's `mark_all_dirty` counterpart) | `cacheClear residual` |
| after tree.clear | `TaffyTree.clear()`: node storage dropped | `treeClear residual` (and `drop` after the fixture reference dies) |

Protocol details:

* Each scenario is warmed up once on a throwaway tree (class loading, JIT, first cache
  allocations) before the measured baseline.
* `clearAllCaches` walks the tree with a reusable deque instead of a pre-collected
  `NodeId[]`, so the fixture does not add ~4 B/node to the sampled tree.
* Rounds are independent: each builds a fresh tree, and the fixture dies before the next
  baseline. The reported number is the median; spreads are printed as `noise`.
* Machine: macOS 26.6.2 (aarch64), 10 CPUs, 16 GiB RAM, JetBrains JDK 17.0.14, G1,
  `-Xmx4g -Xss512m`. All numbers are used-heap deltas on this setup only. A different GC, heap
  size, JDK or architecture (compressed oops on/off) changes every number.

The three shapes are the existing benchmark shapes: `PhaseSupport.buildFlat(flex, 100000)`,
`Ldlib2UiBenchmark.buildForm(20000)` and `PhaseSupport.buildDeep(flex, depth=49999)`
(100,000 nodes). `flat-flex-100k-styled` is the same flat shape with a per-leaf
`TaffyStyle.copy()`, i.e. what a real element tree owns; the shared-style variant exists to keep
the benchmark builders comparable.

## Measured footprint (current checkout)

`./gradlew memoryFootprint` (5 rounds; per-node values are medians, spread in MiB):

| Shape | Nodes | Tree | Layout increment | Warm total | `clearCache` residual | `tree.clear()` residual |
|---|---:|---:|---:|---:|---:|---:|
| `flat-flex-100k` | 100,000 | 727 B/node (69.35 MiB, 0.08) | 648 B/node (61.77 MiB, 0.09) | 1,375 B/node | 1,281 B/node (122.16 MiB, 0.27) | ~2.3 KB |
| `flat-flex-100k-styled` | 100,000 | 1,439 B/node (137.25 MiB, 0.18) | 649 B/node (61.86 MiB, 0.22) | 2,088 B/node | 1,993 B/node (190.06 MiB, 0.13) | ~2.3 KB |
| `form-20k` | 20,055 | 1,512 B/node (28.92 MiB, 0.15) | 221 B/node (4.22 MiB, 0.20) | 1,733 B/node | 1,537 B/node (29.40 MiB, 0.12) | 0.38 MiB |
| `deep-100k` | 100,000 | 1,113 B/node (106.14 MiB, 0.10) | 880 B/node (83.95 MiB, 0.27) | 1,993 B/node | 1,755 B/node (167.35 MiB, 0.28) | ~0.2 KB |

Reading the table:

* **The styled line is the realistic flat number.** `PhaseSupport.buildFlat` reuses one
  `TaffyStyle` instance for all 99,999 leaves, so the 727 B/node excludes per-element styles.
  Copying the style per leaf adds 712 B/node: a `TaffyStyle` is ~200 B shallow and `copy()`
  allocates nine box structs plus nine lists (`TaffyStyle.java:550`). A real 100k-element UI
  therefore starts at ~1.44 KB/node before layout.
* **Form is the heaviest per node** (1.51 KB/node) because `buildForm` creates a style per label,
  row and panel, a `GlyphTextMeasure` per label, and 20,055 node records for 20,051 reachable
  nodes plus the detached row. The fixture also keeps its own `NodeId[]` (~4 B/node), so the
  1.51 KB is an upper bound of the tree alone.
* **Deep is between the two**: 50k container styles and 50k item lists, but only one style for
  all leaves.
* **`tree.clear()` gives the memory back.** Flat and deep return to ~2 KB residual; the probe's
  own references are gone (`drop` residual is also 0). Form keeps 0.38 MiB because the fastutil
  `nodeContextData` map held 15k measure callbacks and `clear()` keeps the table capacity
  (see [What clears what](#what-clears-what)).

Example console output of the captured run:

```
scenario=flat-flex-100k targetNodes=100000 space=Definite(2000.0) x MaxContent
  median (nodes=100000): tree=+69.35 MiB (727 B/node) | layout=+61.77 MiB (648 B/node) | cacheClear residual=+122.16 MiB (1281 B/node) | treeClear residual=+0.00 MiB | drop residual=+0.00 MiB
scenario=form-20k targetNodes=20000 space=Definite(1920.0) x Definite(1080.0)
  median (nodes=20055): tree=+28.92 MiB (1512 B/node) | layout=+4.22 MiB (221 B/node) | cacheClear residual=+29.40 MiB (1537 B/node) | treeClear residual=+0.38 MiB | drop residual=+0.00 MiB
scenario=deep-100k targetNodes=100000 space=Definite(2000.0) x MaxContent
  median (nodes=100000): tree=+106.14 MiB (1113 B/node) | layout=+83.95 MiB (880 B/node) | cacheClear residual=+167.35 MiB (1755 B/node) | treeClear residual=+0.00 MiB | drop residual=+0.00 MiB
# style cost: per-leaf TaffyStyle.copy() adds +67.90 MiB (712 B/node) on the 100k flat shape
```

## Cache ceiling: `fill=8`

A tree asks for at most nine distinct measure results per node (`LayoutCache` keeps a final
layout entry plus nine measure entries, `LayoutCache.java:40`). One layout at one viewport size
fills only the entries that pass needed; a UI that resizes or animates between widths can fill
more. `-PmemoryFootprintFill=8` lays the same tree out at eight more widths after the first
layout, so the extra bytes are the per-node measure-cache ceiling:

| Shape | First layout | +8 resizes | Warm ceiling | `clearCache` residual after fill |
|---|---:|---:|---:|---:|
| `flat-flex-100k` | 647 B/node | +1 B/node (~0) | 1,377 B/node | 1,280 B/node |
| `flat-flex-100k-styled` | 648 B/node | +1 B/node (~0) | 2,089 B/node | 1,996 B/node |
| `form-20k` | 229 B/node | **+323 B/node** | **2,056 B/node** | 1,750 B/node |

These are medians of a separate 3-round fill run, so a value can differ from the 5-round table
above by ~0.1% of the total (GC noise). Flat leaves have definite 10x10 sizes, so a resize
changes the parent's layout but never creates a new measure input: the ceiling equals the first
layout. Form's 15,003 labels are measured leaves and do fill entries (~15k labels x up to 9
entries = 6.5 MiB, hence +323 B/node). `deep-100k` was also run with `fill=8`, but one round's
baseline was not collected (layout spread 18.6 MiB), so no number is quoted; structurally the
deep chain has no measure callbacks and each resize overwrites its single final entry, so the
ceiling equals the normal 1,993 B/node. This is why a "warm form" figure in review can be
higher than this document's 1,733 B/node: it depends on whether measure entries have been filled
(and whether the first layout ran in a cold JVM; a throwaway cold-JVM first layout measured
~780 B/node on form, versus 221 B/node after JIT/cache warm-up).

## Old (1.2.0-local5) vs current

The published `dev.vfyjxf:taffy:1.2.0-local5` predates the flex/block item-list caches. The same
compiled probe runs in a second JVM whose classpath puts the old all-jar where the current main
classes would be (the probe only uses APIs that exist in both versions):

```bash
JMH_CORE=$(find ~/.gradle/caches -name 'jmh-core-1.37.jar' | head -1)
OLD=~/.m2/repository/dev/vfyjxf/taffy/1.2.0-local5/taffy-1.2.0-local5-all.jar
/Users/vfyjxf/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home/bin/java \
  -Xss512m -Xmx4g -cp "build/classes/java/jmh:$JMH_CORE:$OLD" \
  dev.vfyjxf.taffy.benchmark.MemoryFootprintMain rounds=5 label=local5
```

Same protocol, medians of 5 rounds (per-node bytes):

| Shape | Tree old -> current | Layout incr old -> current | Warm old -> current | `clearCache` residual old -> current | `tree.clear()` old -> current |
|---|---|---:|---:|---:|---:|
| `flat-flex-100k` | 811 -> **727** (-84) | 296 -> **648** (+352) | 1,107 -> **1,375** (+268) | 1,011 -> **1,281** (+270) | 12.00 MiB -> **~0** |
| `flat-flex-100k-styled` | 1,523 -> **1,439** (-84) | 296 -> **649** (+353) | 1,819 -> **2,088** (+269) | 1,724 -> **1,993** (+269) | 12.00 MiB -> **~0** |
| `form-20k` | 1,519 -> **1,512** (-7) | 448 -> **221** (-227) | 1,967 -> **1,733** (-234) | 1,765 -> **1,537** (-228) | 1.13 MiB -> **0.38 MiB** |
| `deep-100k` | 1,193 -> **1,113** (-80) | 726 -> **880** (+154) | 1,919 -> **1,993** (+74) | 1,680 -> **1,755** (+75) | 12.00 MiB -> **~0** |

Fill ceiling (`fill=8`, medians of 3 rounds): flat 1,107 -> 1,377; styled 1,819 -> 2,089;
form 2,394 -> 2,056 bytes/node. Deep does not grow under the resize layouts; its ceiling is the
normal 1,993 B/node (1,919 for local5).

What the deltas mean:

* **Building moved from hash maps to dense arrays.** local5 keeps every node in two
  `Long2ObjectOpenHashMap` tables (`nodes`, `parents`); the current tree indexes
  `NodeData[]`/`NodeId[]` by id (`TaffyTree.java:79`). That is why build is 80-84 B/node
  smaller on the 100k shapes.
* **`clear()` finally returns memory.** local5's `clear()` only calls `Map.clear()`; fastutil
  keeps its key/value tables, so 100k nodes leave 12.00 MiB behind (126 B/node) and 20k leave
  1.13 MiB. The current `clearNodeStorage()` replaces the arrays with 16-slot arrays
  (`TaffyTree.java:331`), which is the ~0 residual in the table. Form still keeps 0.38 MiB
  because its context maps (`nodeContextData`, 15k measure callbacks) are only `clear()`ed.
* **The item-list caches are the memory price of the p20 optimization.** Flat flex goes from
  296 to 648 B/node of layout state and the deep chain pays +154; the flat 100k-child flex root
  keeps one item list with ~100k `FlexItem` metadata objects. Form moves the other way: the
  current checkout stores 227 B/node less on first layout (and 338 B/node less at the fill
  ceiling). Time numbers belong to the JMH docs; this document only records the memory side of
  that trade.

## Cache upper-bound estimates

The formulas below are field-layout estimates for HotSpot 64-bit with compressed oops
(12 B object header, 4 B references, 8 B alignment), not counted object graphs. The measured
tables above are the authority; these are useful for sizing a new container shape.

| Structure | Estimate | Notes |
|---|---|---|
| Per-node `NodeData` | ~80 B shallow + ~24 B `NodeId` | fields at `NodeData.java:12`; `children` is an `ArrayList` (24 B + element array) |
| Per-node `LayoutCache` | 48 B + 56 B (`CacheEntry[9]`) = **104 B** always | allocated in every `NodeData`; entry slots start null |
| Final layout entry | 48 B `CacheEntry` + 48 B `LayoutOutput` + ~150-250 B components | one per laid-out node; `LayoutCache.java:168` |
| Measure entries | up to 9 x (48 + 16) = **576 B/node** | only nodes that may be measured; filled as distinct inputs arrive |
| Context-free entries | **0 B extra** | one boolean + one int on `LayoutCache`; they only unlock final-entry reuse, no new object |
| `BlockItemCache` | ~56 + (16+8n) + 24 + (16+4n) + n x ~400 = **~112 + ~412n B** | n = direct in-flow children; `BlockItem` has 24 refs, 9 floats, 1 int, 14 booleans (`BlockComputer.java:357`) |
| `FlexItemCache` | ~56 + (16+8n) + (16+4n) + 24 + (16+4n) + n x ~460 = **~128 + ~476n B** | n = flex items; the flat 100k-child root is ~47 MB by this formula and measured residual supports ~55 MB |
| Dense id arrays | **8 B/slot** (`NodeData[]` + `NodeId[]`) | 100k nodes grow to 132,340 slots (~1.06 MB); capacity capped at `max(1024, live*8)` (`TaffyTree.java:344`), so sparse id histories spill to a map instead of growing |
| `TaffyStyle` per element | ~200 B shallow; **~712 B** with `copy()` | measured styled minus shared style |

Per tree: `tree = nodes x (80 + 24 + 104 + 8 + style) + container item caches`; laid-out:
add per-node final entry and (for measured leaves) up to 576 B/node. The 100k flat measurement
(727 B/node built, 1,375 B/node warm) shows the fixed per-node part dominates for small styles,
while the flat flex root's item cache dominates after layout.

## What clears what

| Operation | Layout entries | `BlockItemCache` | `FlexItemCache` | `Layout` records | Node storage |
|---|---|---|---|---|---|
| `clearCache(node)` (`TaffyTree.java:1412`) | invalidated in place, **shells kept** (`LayoutCache.java:610`) | cleared (`:1418` → `BlockComputer.java:1354`) | **not cleared** | kept on `NodeData` | kept |
| `setStyle` / child-list change | invalidated in place, shells kept | dropped on the node | dropped on the node | kept | kept |
| `tree.clear()` (`TaffyTree.java:629`) | all nodes dropped with the arrays | dropped | dropped | dropped | arrays reset to 16 slots (`:331`); context map tables keep capacity |

Measured consequences (per-node, from the tables above):

* **`clearCache` is a correctness invalidation, not a release.** Flat's warm 1,375 B/node only
  falls to 1,281 after clearing every node (-94 B/node); form 1,733 -> 1,537; deep 1,993 -> 1,755.
  The flex root's item list (~55 MB on the flat shape) stays pinned because `clearCache`
  clears only the block item list; version stamps keep the retained list correct, it is released
  when the container's next flex layout replaces it or its style/children change
  (`FlexboxComputer.java:1043`).
* **Emptying a tree returns its storage**, but the caller still owns styles, measure callbacks
  and `NodeId` handles. Form's 0.38 MiB residual is the fastutil table of
  `nodeContextData`/`nodeMeasureContexts` after `clear()`; a UI tree with 15k measured labels
  has the same effect.

## Risks and measurement limits

* **`System.gc()` is advisory.** Three calls plus 30 ms usually produce a full G1 collection,
  but a sample can still include uncollected garbage. One `deep-100k` fill round showed a
  non-collected sample (~19 MiB spread); medians and spreads are reported instead of single
  runs, and anything below ~0.3 MiB per scenario is noise. Do not quote a single round.
* **Used heap is not an object graph.** Deltas include JVM-internal retention, allocator
  rounding and GC slack. For exact object sizes run `jcmd <pid> GC.class_histogram` or
  `jmap -histo:live`; numbers here size a feature, they are not a leak-detector baseline.
* **Per-node divides by `totalNodeCount()`.** `form-20k` includes the detached row and the
  fixture's `NodeId[]` (`BaseShape`, ~4 B/node); the per-node number is therefore an upper
  bound of the tree alone.
* **The flat benchmark shares one leaf style.** Use `flat-flex-100k-styled` (or form) as the
  realistic per-node figure; the shared-style line is only comparable with the JMH shapes.
* **The stack requirement is real.** `deep-100k` is a ~50k-frame recursion; the task runs with
  `-Xss512m`, while the JMH tasks use `-Xss16m`. A 100k-deep tree must not be built under the
  JMH stack setting. The same applies to any profiling tool that uses the JMH launcher.
* **Caches scale with fan-out.** A 100k-child flex container keeps a ~55 MB item list and
  `clearCache` will not drop it. Virtualized lists that rebuild a container's children stay
  bounded; a single flat container with 100k children does not. Prefer many smaller containers
  when the UI can be structured that way.
* **Fill runs on deep are expensive and noisy.** `-PmemoryFootprintFill=8` adds eight full
  layouts per round; on the 50k-deep chain that is ~8x the layout work and the heap sample is
  the same one-GC-per-point protocol, so a non-collected sample is possible. Use fill for form
  and flat when characterizing the measure-cache ceiling; deep's ceiling does not grow.
* **Old-jar comparison needs the same JVM.** `local5` numbers are only meaningful next to the
  current ones because both ran on JDK 17/G1 with the same `-Xss512m -Xmx4g`; the old jar must
  not be mixed with current main classes on the classpath.

## Verification

Captured on the setup above:

* `./gradlew jmhClasses` passes (the probe compiles with the JMH source set and its annotation
  processor; it defines no `@Benchmark` and its name matches none of the JMH task include
  patterns, so JMH discovery and the existing tasks are unchanged).
* `./gradlew memoryFootprint` (5 rounds x 4 shapes) completes in ~30 s and produced the tables
  above; `-PmemoryFootprintFill=8` (3 rounds) produced the ceiling table.
* The `local5` comparison ran in a separate JVM from the same compiled classes and completed
  with the same protocol; its medians are the old column of the comparison table.
* No JMH benchmark was started for this document; the probe deliberately avoids the runner.
