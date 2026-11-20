# LDLib2 UI scenarios (JMH)

`Ldlib2UiBenchmark` is the "real downstream UI" layer of the benchmark suite. The other classes
([perf-benchmarks.md](perf-benchmarks.md), [perf-scenarios.md](perf-scenarios.md)) answer
"is flex/grid/block/cache fast"; this class answers "is the layout work an actual LDLib2 screen
does per frame fast". Every tree is derived from a concrete LDLib2 widget path, every operation
mirrors one step of `ModularUI.calculateStyleAndLayout`, and every benchmark carries a comment
pointing at the LDLib2 source it models.

`LdlibUiBenchmark` (the older, generic mixed-panel benchmark) is intentionally kept separate: it
covers cosmic_text-style measure-heavy and percentage/aspect shapes, while this class owns the four
downstream UI shapes and the per-frame operations below.

LDLib2 reference checkout: `LDLib2` at `6cc1f97` (v2.2.46.a), read-only. The line references
below are from that checkout.

## What LDLib2 does per frame

| LDLib2 behaviour | Source | Consequence for the benchmark |
|---|---|---|
| Layout loop: clean the style engine, lay out while the taffy root is dirty, then dispatch `onLayoutChanged` | `ModularUI.java:616-650` | one frame is one or more `computeLayout` calls; a dirty style engine can force a second pass |
| Rounding is disabled on the UI tree | `ModularUI.java:211` (`taffyTree.disableRounding()`) | every tree in this class disables rounding, so numbers are comparable to LDLib2's layout |
| Root available space comes from the root element's width/height style (`percent` to screen size, `auto` to `MAX_CONTENT`) | `ModularUI.java:561-585` | the root is percent-sized and the viewport size is the benchmark's available space |
| A style write dirties the element and its ancestors through `markTaffyStyleDirty` | `UIElement.java:342`, `onStyleChanged` at `UIElement.java:1173` | per-frame hover/animation cost is `setStyle` + `markDirty` walk + layout cache checks |
| Labels measure their text and write the measured width/height back into the layout style (`adaptiveWidth` / `adaptiveHeight`), i.e. a second layout pass runs | `TextElement.java:200-224` | measured leaves are central; the benchmark uses a deterministic glyph model instead of font rendering |
| `Label` defaults to a 9px line box | `Label.java:30` | line height 9 in the glyph model |
| `ScrollerView`: viewport with `overflow` clipping (padding 5), view container, scrollbars whose display/margin are rewritten after layout | `ScrollerView.java:155-190` structure, `:173` clip, `:273-330` `updateScrollers`, `:201-210` scroll offset via `layout.top(...)` | the scroll shape nests clipped viewports and moves the view container with a relative inset |
| `InventorySlots`: three 9-slot player rows plus a hotbar row; each `ItemSlot` is a fixed 18x18 element | `InventorySlots.java:28-45`, `:83-93`, `ItemSlot.java:172-173` | the inventory shape uses six 9-slot rows of 18x18 slots |
| `display: GRID` panels with fixed/`1fr` tracks | `LayoutPanel.java:24-28` | the dashboard shape is a grid card wall with `1fr` columns |
| Tooltips/overlays are out-of-flow elements positioned with `position: absolute` + `left/top` | `WindowTooltipBoundsScenario.java:161-168` | the dashboard badge and inventory tooltip are absolutely positioned children |

## Shapes, methods and parameters

All classes live in `src/jmh/java/dev/vfyjxf/taffy/benchmark/Ldlib2UiBenchmark.java`.
Every state builds its tree in `@Setup(Level.Trial)`. The cold states (including `measureCold`)
clear every node's cache in `@Setup(Level.Invocation)`; `measureWarm` clears only the root cache
there; no other state touches a cache.

| Shape | Tree (node counts) | Parameters | Methods |
|---|---|---|---|
| **form** — config/menu page | block root (padding 8, 1920x1080) to `panels` x block panel (padding 8, border 2) to 100 x flex row (gap 4) to 3 measured labels; `panels = ceil(targetNodes / 401)`, nodes `1 + 401 * panels` = **1,204 / 5,214 / 20,051** | `targetNodes` = 1000 / 5000 / 20000 | `formColdLayout`, `formPerFrameStyleChange`, `formResizeRoot`, `formAddRemoveChild` |
| **dashboard** — grid card wall | grid root (1280x720, padding 8, gap 6, `1fr` columns, `minmax(56px, auto)` rows) to `side^2` x flex-column card (padding 6) to 2 measured labels + absolute badge (24x12, top/right 4); nodes **257 / 1,025** | `side` = 8 / 16 | `dashboardColdLayout`, `dashboardPerFrameStyleChange`, `dashboardResizeRoot`, `dashboardAddRemoveChild` |
| **scroll** — nested scrollers | `overflow: scroll` root (900x600, padding 5, scrollbar 8) to view container to clipped scroll container (height 380, scrollbar 6, padding 4) to view to clipped scroll container (height 240, scrollbar 6, padding 4) to view to 5,000 fixed 12px rows; **5,006 nodes** | `contentRows` = 5000 | `scrollColdLayout`, `scrollPerFrameStyleChange`, `scrollResizeRoot`, `scrollAddRemoveChild` |
| **inventory** — container screen | flex-column root (400x400, padding 12, gap 4) to 6 x flex row (gap 2, centered) to 9 x 18x18 slot (border 1) + absolute tooltip (140x48, inset 230/70) with 2 measured lines; **64 nodes** | none | `inventoryColdLayout`, `inventoryPerFrameStyleChange`, `inventoryTooltipFollow`, `inventoryResizeRoot`, `inventoryAddRemoveChild` |
| **measure** — text-heavy list | flex-column root (width 900) to 2,000 x percent-width measured label with auto height; **2,001 nodes** | `labelCount` = 2000 | `measureCold`, `measureWarm` |

Totals: **19 `@Benchmark` methods, 31 benchmark runs** (form 4x3, dashboard 4x2, scroll 4x1,
inventory 5x1, measure 2x1).

The form shape is specified as "20 panels x 100 rows x 3 labels, about 6k nodes" (8,021 nodes at 20
panels). The three `targetNodes` values scale the panel count (3 / 13 / 50 panels) instead of the
row or label count, so every variant keeps the documented 100-row x 3-label leaf shape while landing
near 1k / 5k / 20k nodes.

## Operations

| Operation | What the measured method does | Which caches are valid | LDLib2 counterpart |
|---|---|---|---|
| `*ColdLayout` | `computeLayout(root, availableSpace)` + read back sampled layouts | none: `@Setup(Invocation)` calls `clearCache` on every node (upstream `mark_all_dirty`) | first frame / full invalidation, `ModularUI.java:616-630` |
| `*PerFrameStyleChange` | `setStyle(oneLeaf, alternateStyle)` then `computeLayout` | all untouched subtrees | hover/animation: `UIElement.onStyleChanged` to `markTaffyStyleDirty` (`UIElement.java:1173`, `:342`) |
| `*ResizeRoot` | `computeLayout` with the alternate available width | untouched fixed-size subtrees | `setScreenSize` recomputes layout width and re-runs the loop (`ModularUI.java:561-595`) |
| `*AddRemoveChild` | `addChild` to `computeLayout` to `removeChild` | the pre-built detached child keeps its own cache (a reused list row) | `UIElement.addChild` mirrors into taffy (`UIElement.java:867-911`) |
| `measureCold` | `computeLayout` on the measured-label tree | none: every label cache cleared | `TextElement.recompute` measure on a cold frame (`TextElement.java:200-224`) |
| `measureWarm` | same tree, only the root cache cleared | every label | the second pass of the dirty loop: `computeLayout` must not re-measure (`ModularUI.java:618-650`) |

Details that keep the measurements honest:

* **The tree is built once per trial.** No operation rebuilds nodes; `addRemoveChild` re-attaches a
  node that was built in trial setup.
* **Cold means "caches cleared in invocation setup".** No benchmark clears a cache inside the
  measured method, and no non-cold benchmark clears caches at all, so the incremental numbers are
  incremental.
* **The layout output is read back.** Each measured method folds 96 sampled node layouts (location
  and size) into a float consumed by the `Blackhole`; a real UI consumes every node's layout to
  render it. Without the read-back JMH could delete the layout call.
* **Deterministic text.** `GlyphTextMeasure` uses a 7px advance, a 9px line and at most two lines,
  with character counts derived from `8 + (i*37) mod 56`. There is no clock, no randomness and no
  string work in any state, so two independently built trees produce the same checksum (asserted by
  the smoke harness).
* **Rounding is off**, matching `ModularUI.java:211`.

The form label measure is also where `TextElement.recompute`'s feedback loop shows up: the measured
height changes the label's layout height, which is what forces LDLib2's second `computeLayout`
pass. The benchmark models the *result* of that pass rather than the style engine.

## Commands

```bash
# Compile the benchmark sources (also the fastest sanity check after editing the scenarios)
./gradlew jmhClasses

# Plain-java smoke: one invocation of every method/parameter pair, structural checks only.
# This does not start JMH and produces no measurement.
./gradlew jmhSmokeLdlib

# All LDLib2 scenarios (JMH; default 1 warmup + 1 measurement, 1 fork)
./gradlew jmhLdlib

# One shape
./gradlew jmhLdlib -PjmhLdlibIncludes='.*Ldlib2UiBenchmark\.(form|dashboard|scroll|inventory).*'

# One method (all of its parameters)
./gradlew jmhLdlib -PjmhLdlibIncludes='.*Ldlib2UiBenchmark.formColdLayout'

# One method with one pinned parameter
./gradlew jmhLdlib -PjmhLdlibIncludes='.*Ldlib2UiBenchmark.formColdLayout' \
    -PjmhLdlibParams='targetNodes=20000'

# Stable comparison run
./gradlew jmhLdlib -PjmhLdlibWarmupIterations=3 -PjmhLdlibIterations=5 -PjmhLdlibFork=2
```

Results: `build/reports/jmh/results-ldlib.json` and `build/reports/jmh/output-ldlib.txt`.
The class is also part of the default `jmh` task, because its name matches `.*Benchmark.*`.

Run the shapes **serially**, one Gradle invocation at a time: the form class builds 12 trial trees
across its 12 runs (four of them at 20k nodes, one per method), and two JVMs doing that at once only
adds allocation noise. For a single hotspot profile, pin one method (and one parameter when
relevant) instead of running the whole class:

```bash
./gradlew jmhLdlib -PjmhLdlibIncludes='.*Ldlib2UiBenchmark.formColdLayout' \
    -PjmhLdlibParams='targetNodes=20000'
./gradlew jmhLdlib -PjmhLdlibIncludes='.*Ldlib2UiBenchmark.scrollColdLayout'
./gradlew jmhLdlib -PjmhLdlibIncludes='.*Ldlib2UiBenchmark.measureCold'
```

## Expected hotspots

These are the code paths each scenario is expected to exercise. They are hypotheses to confirm with
a profiler, not conclusions; use a phase class from [perf-scenarios.md](perf-scenarios.md) to split a
suspicious number into its stage.

| Scenario / operation | Expected to dominate |
|---|---|
| `formColdLayout` | block content sizing + flex row algorithms over the full tree, per-node cache store, `MeasureFunc` calls (one to three per label: min-content, max-content, definite width), style value resolution for padding/border/gap |
| `formPerFrameStyleChange` | `TaffyTree.setStyle` (store + direction epoch bump), `markDirty` ancestor walk, cache-key comparisons on the path to the root, re-measure of the one hovered label; everything else should be a cache hit |
| `formResizeRoot` | root/percent re-resolution and cache misses caused by the new available width on the responsive path; fixed-size rows should stay cached |
| `formAddRemoveChild` | parent child-list mutation, dirty propagation to the panel/root, measure of the three added labels; the detached row keeps its cache, as a reused list row would |
| `dashboardColdLayout` | grid track sizing (`1fr` columns, `minmax` auto rows), implicit row/column placement, out-of-flow badge exclusion from track sizing, measured label contribution |
| `dashboardPerFrameStyleChange` | dirty walk + cache checks with almost no geometry change (out-of-flow badge), the cheapest per-frame animation case |
| `dashboardResizeRoot` | `1fr` track re-resolution for every column and row |
| `scrollColdLayout` | scrollable-overflow computation (`scrollWidth`/`scrollHeight`), scrollbar gutter reservation on three nested `overflow: scroll` containers, 5,000-row flex column |
| `scrollPerFrameStyleChange` | relative-inset write on the view container, dirty walk, cache checks for the 5,000 rows (should be hits) |
| `scrollResizeRoot` | percent-width re-resolution inside the scroll containers, scrollbar presence changes |
| `inventoryColdLayout` | many tiny fixed-size leaves, absolute tooltip positioning, flex row centering |
| `inventoryPerFrameStyleChange` | hover style write on an 18x18 border-box slot, dirty walk, cache checks for 54 siblings |
| `inventoryTooltipFollow` | absolute inset re-resolution for one out-of-flow node, dirty walk; slot rows should not be touched |
| `measureCold` | `LayoutComputer` measure path: measure-callback dispatch, `FloatSize` allocation per call, cache store per measured leaf (3 measure calls per label observed: min-content, max-content, definite) |
| `measureWarm` | root algorithm + cache lookups only; the measure callback must not run (smoke asserts 0 calls) |

## Smoke evidence

The smoke harness checks structure and response, not speed. It reports 55 checks, 0 failures:

```
form/1000|5000|20000   nodes 1204 / 5214 / 20051; root 1920x1080; hover height 9 to 13; resize 1920 to 1600; rows restored to 100
dashboard/8|16         nodes 257 / 1025; root 1280x720; badge 24 to 30; resize 1280 to 1024; cards restored to 64 / 256
scroll/5000            nodes 5006; root 900x600; view container y 5 to -115 (relative -120 offset); resize 900 to 1100; rows restored to 5000
inventory/9x6          nodes 64; root 400x400; hovered content box 16 to 12; tooltip 230,70 to 234,74; resize 400 to 480; slots restored to 9
measure/2000           nodes 2001; cold measure calls 6000; warm measure calls 0
form/determinism       two independently built trees produce the same layout checksum
```

## Limitations

* The style engine (`StyleBag`, selector matching, style animations) is **not** modelled. A style
  change is a `TaffyTree.setStyle` call; the cost of resolving the style cascade is covered by
  `StylePhaseBenchmark` in the phase suite, not here.
* Text measurement is a glyph model (7px/char, 9px line, max two lines). It captures the control
  flow and the width dependence of `TextElement.recompute`, not real font metrics.
* The trees are static apart from the one leaf each per-frame operation touches; a frame that
  animates hundreds of elements is a composition of the single-leaf case, not a new shape.
* These numbers are not comparable to the Rust/`jaffy` benches in `perf-benchmarks.md`. They exist to
  tell a downstream regression ("a form screen is slower") apart from a core regression (a phase
  number moved), and to give a profiler a representative stack.
