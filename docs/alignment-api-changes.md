# Alignment API changes: breaking changes and deprecations

> Status: 2026-11-19, `align/upstream-rust` @ `ee37aa2`, 2,893 tests green.
> This single file tracks every **source/binary/behavior break** and **deprecated API**
> introduced by the upstream-alignment work, plus what the remaining phases will add.
> Per-value layout changes live in [`compat-golden/ALLOWLIST.md`](../compat-golden/ALLOWLIST.md);
> the user-facing upgrade guide is [`migration-1.2.md`](migration-1.2.md).

## 1. Remaining phases

P0-a, P0-b, P1 and P2 are complete. P3 is mostly complete.

| Batch | Scope | Status | Expected API impact |
| --- | --- | --- | --- |
| P3 residual | BK-14 (block `align-content`), grid baseline A3-A6 / #1322-#1323 | done (tag `p3-complete`) | behavior only |
| **P4** | SO-01..SO-05 scrollable overflow | done (tag `p4-complete`) | behavior; fills the new `ScrollableOverflow` component; `scrollWidth/Height` switch to it |
| **P5** | ST-02/03/04/08, FL-15..20, BK-16/19 | done (tag `p5-complete`); ST-08 + FL-15/16 deferred by decision/dependency | more enum constants (minor source break); opt-in keyword mode |
| **P6** | OF-01..07, ST-06, FL-22/23, GR-19, TS-05 | partial (tag `p6-complete`); OF-01/TS-05 deferred by D2 to 1.4/2.0 | `TaffyPosition` STATIC/FIXED/STICKY (source break); abspos semantics |
| **P7** | FT-01..07, BK-18/20, ST-07, FL-21/25/26, GR-01..04/06/07/11..13/15..18, TS-06 | not started | float layout, grid track types, DetailedGridInfo; largest batch |

So there are **four full phases left (P4-P7) plus the P3 residual segment**.

## 2. Already introduced in 1.2.0

### 2.1 Source-breaking

| Change | Why | Mitigation |
| --- | --- | --- |
| `AlignSelf.START`, `AlignSelf.END` added (appended after `STRETCH`) | PA-02: `start/end` must stay distinct from `flex-start/flex-end` under `wrap-reverse` | Existing constants keep names/ordinals; exhaustive `switch` **expressions** over `AlignSelf` may need the new cases or a `default` (Q2-approved minor exception) |
| `TaffyDisplay.FLOW_ROOT` added (appended after `NONE`) | BK-17: `display: flow-root` | Same enum caveat; `TaffyDisplay.DEFAULT` is still `FLEX` |
| `Layout` gains component `scrollableOverflow` | T1/BL-03: new overflow contract | Old 8-arg constructor and `contentSize()` accessor retained; canonical record constructor, `equals/hashCode/toString` and JSON shape change |
| `LayoutOutput` gains components `scrollableOverflow`, `baselines` | T1/BL-03 + T2/BL-02 | Old 6-arg constructor and `contentSize()`/`firstBaselines()` accessors retained; same record/JSON caveat |
| `printTree` debug output format | CR-05: print every `Layout` field | Signature unchanged; output is debug-only, not a layout contract |

All enum additions are **binary compatible**. All record additions keep the old public
constructors/accessors, so code compiled against 1.1.5 still links (verified by
`checkApiCompatibility` and javap in `RecordSchemaTest`).

### 2.2 Behavior changes (not API breaks)

These change computed numbers, not signatures. Every change that intersects the frozen
golden scenarios has an old/new row in `compat-golden/ALLOWLIST.md`.

- **P0-a bug fixes**: grid abspos named lines, AlignSelf START/END, RTL root, `copy()`
  flex shorthand, definite-final-size abspos second pass, block `display:none`
  descendants.
- **P0-b**: `Layout.margin` now reports the box's own margins (BK-10); `TaffyTree.remove`
  marks the parent dirty; cache keys include parent size/mode and measure entries with
  collapse metadata are not cached (hit behavior/perf changes).
- **P1**: flex intrinsic/shrink/clamping/wrap/alignment fixes; block percentage bases,
  min-height collapse-through, cumulative rounding (BK-13; two generated expectations and
  one relayout expectation were updated); grid track/line/span clamps, empty-axis
  alignment, abspos track edges.
- **P3**: block percentage padding/inset/height use the content box; collapse-through uses
  the used height; flex items subtract margins from available space; auto-cross-margin
  items leave baseline alignment; `flow-root` margin isolation (float containment is
  still F7/P7); compressible-replaced `%max` cap (BK-12); fixed-size leaves consult the
  measure callback for baselines.

### 2.3 Deprecated APIs actually annotated

| Deprecated member | Replacement | Kept until |
| --- | --- | --- |
| `LayoutCache.get(FloatSize, TaffySize<AvailableSpace>, RunMode)` | `get(LayoutInput)` | 2.0.0 |
| `LayoutCache.store(FloatSize, TaffySize<AvailableSpace>, RunMode, LayoutOutput)` | `store(LayoutInput, LayoutOutput)` | 2.0.0 |
| `TaffyTree.getCacheEntry(NodeId, FloatSize, TaffySize<AvailableSpace>, RunMode)` | `getCacheEntry(NodeId, LayoutInput)` | 2.0.0 |
| `TaffyTree.storeCacheEntry(NodeId, FloatSize, TaffySize<AvailableSpace>, RunMode, LayoutOutput)` | `storeCacheEntry(NodeId, LayoutInput, LayoutOutput)` | 2.0.0 |

Unrelated, pre-existing deprecations (`TaffyTree.hasUnconsumedLayout`,
`RoundLayout.roundLayout(tree,node,x,y)`) are not part of this alignment work.

## 3. Deprecations planned but not yet annotated

These APIs are already superseded but intentionally still plain (no `@Deprecated`) to
avoid warning noise while internal call sites migrate; they must be annotated before the
1.2.0 release and removed in 2.0.0.

| Legacy API | Replacement | Plan |
| --- | --- | --- |
| `Layout` 8-arg constructor + `contentSize()` | canonical 9-arg + `scrollableOverflow()` | deprecate in 1.2.x, remove in 2.0.0 |
| `LayoutOutput` 6-arg constructor + `contentSize()`/`firstBaselines()` | canonical 8-arg + `scrollableOverflow()`/`baselines()` | same |
| `MeasureFunc` + `TaffyTree.newLeafWithMeasure/setMeasureFunc/getMeasureFunc` + `computeLayoutWithMeasure` | `MeasureFunction`/`MeasureContext` + `newLeafWithMeasureFunction/setMeasureFunction/getMeasureFunction` + `computeLayoutWithMeasureFunction` | annotate in 1.2.x, remove in 2.0.0 |
| `AlignItems.AUTO` | `NORMAL` | remove in 2.0.0 |

## 4. What the remaining phases will add

| Batch | Expected source/binary breaks | Expected deprecations/removals |
| --- | --- | --- |
| P3 residual | none (behavior only) | none |
| P4 | none planned; fills `Layout.scrollableOverflow` | none until 2.0.0 (`contentSize` removal) |
| P5 | `AlignItems`/`AlignContent` `NORMAL`/`SAFE_*`, `FlexWrap.BALANCE`, sizing-keyword enums (appended) | none in 1.x; opt-in keyword mode in 1.3, default/type swap in 2.0.0 |
| P6 | `TaffyPosition.STATIC/FIXED/STICKY` (appended) | `TaffyPosition.RELATIVE` default becomes `STATIC` in 2.0.0; abspos `location` origin changes in 1.4 preview/2.0.0 default |
| P7 | float layout API, grid track/DetailedGridInfo types | old grid/track APIs (if any) deprecated in 1.2.x, removed in 2.0.0 |

Version policy (Q0-Q4 approved): 1.2.0 bug fixes/additive contracts; 1.3.0 opt-in and
parity subset; 1.4.0 abspos/OOF preview; 2.0.0 flips defaults, removes legacy record
components/constructors/`MeasureFunc`/`AUTO`.

## 5. Compatibility gates and blind spots

- `./gradlew checkApiCompatibility` (JarCompatibilityChecker 0.1.23 vs `dev.vfyjxf:taffy:1.1.5`)
  catches removed members/visibility drops and conditional raw-type/structure changes.
- It **cannot** see enum additions, field/method additions, generic arguments, default
  values or behavior. Those are covered by `RecordSchemaTest` (component order),
  `compat-golden` (behavior) and `CHANGELOG`/migration notes.
- The historical `docs/migration-1.2.md` file is the user-facing guide; this file is the
  maintainer-facing inventory.
