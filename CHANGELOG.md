# Changelog

All notable changes to taffy-java are documented in this file.

## 1.2.0 (unreleased)

First batch of the upstream-alignment work (P0-a): behavior fixes for grid,
flex, RTL roots, style copying, and block hidden descendants. Public API
signatures are unchanged, but layouts that hit the fixed defects change. The
per-value record is [compat-golden/ALLOWLIST.md](compat-golden/ALLOWLIST.md);
the upgrade guide is [docs/migration-1.2.md](docs/migration-1.2.md).

### Added (additive contracts from P0-b)

- `MeasureFunction` + `MeasureContext`: a new measure callback that receives the
  derived `LayoutInput`, the node's `TaffyStyle`, the `NodeId` and an optional
  node context, and returns a `LayoutOutput` (size plus baselines/scrollable
  overflow). `MeasureFunc` remains and is adapted internally; per-node callbacks
  win over tree defaults, and the legacy callback keeps the old inputs.
- `ScrollableOverflow` and `Baselines` value types; `Layout` gains
  `scrollableOverflow`, `LayoutOutput` gains `scrollableOverflow` and
  `baselines`. The legacy `contentSize`/`firstBaselines` components and the old
  6/8-arg constructors and accessors remain for 1.x compatibility. No layout
  algorithm writes the new values yet (P3/P4); the accessors defensively copy
  their mutable geometry.
- `TaffyTree` overloads for the new measure function, node context, and
  `computeLayoutWithMeasureFunction`; old methods are unchanged.
- `LayoutCache`/`TaffyTree` `LayoutInput`-based cache accessors with
  parent-size and mode-aware keys; old signatures delegate.

### P2 additions

- `TaffyTree.writeTree(Writer, NodeId)` writes the full layout tree (order,
  location, size, content size, scrollable overflow, scrollbar, border, padding,
  margin); `printTree` now delegates to it, so the debug output format changed
  while the method signature stayed the same.
- `transpose()` on `TaffySize`, `TaffyRect`, `LayoutInput` and
  `LayoutOutput` for the vertical writing-mode path. Geometry and baselines are
  swapped; collapsible margins and the collapse-through flag are reset, matching
  upstream taffy #1265 (they do not survive a transposition).
- An opt-in generated-test freshness gate:
  `:gentest:checkGeneratedFreshness` with `-Ptaffy.checkGenerated=true`
  (or `TAFFY_CHECK_GENERATED=true`), a method-level
  `gentest/freshness-allowlist.txt` with stale-entry detection, and CI
  instructions in `gentest/README.md`.

### Post-P7 alignment fixes

- Leaf/root available space now deducts a child's margins exactly once (X2/C17),
  fixing double subtraction for block/flex/grid measure callbacks.
- Block stretch width excludes compressible-replaced boxes, and aspect-ratio
  transfer derives either axis before child layout (BK-04/BK-15; the latter is
  gated to final layout until FL-15/BL-01 lands).
- Grid intrinsic measurement subtracts item margins from the available space
  (T24), and BFC float slots include the nested containing block insets.
- The sized `grid-template-areas` value keeps the upstream name
  `GridTemplateAreas`. A consumer that declares the same simple name and
  wildcard-imports `dev.vfyjxf.taffy.style.*` must qualify one of the two; Java has
  no import alias.
- LDLib2 regression run (523 tests, Java 21): all layout tests pass; the three
  Mockito-initialization failures and one `FileDialogPathTest` failure are
  environment/baseline issues unrelated to taffy. Downstream exhaustive switches
  over the extended enums were migrated with `default` (Q2 source-break noted above);
  the wildcard `GridTemplateAreas` ambiguity is left to the consumer to qualify.

### P11 performance additions

- `TaffyTree.roundLayout(NodeId)`: additive public entry point for the layout
  rounding pass that `computeLayout` runs when rounding is enabled.
  `RoundLayout.roundLayout(TaffyTree, NodeId)` delegates to it, so both entry
  points share one implementation. The pass fetches each node's `NodeData` once
  (unrounded layout plus the child list now stored on `NodeData`) and keeps its
  traversal stack in `long[]`/`float[]` arrays instead of `Object[]` frames
  with boxed `Float` coordinates. Rounded values, visit order, listener
  notifications and dirty-flag propagation are unchanged; the deprecated
  four-argument `RoundLayout.roundLayout(tree, node, x, y)` overload keeps its
  original recursive implementation and semantics.
- Grid column sizing indexes items by their start column (instead of scanning
  every item for every track), precomputes track start offsets for placement,
  reuses the known-dimensions cache in the auto-row pass and memoizes grid
  placement parsing per layout pass.
- Block item generation resolves margins once as floats and reuses transient
  geometry; the layout cache stores key fields in place (no per-lookup
  `CacheKey`), reuses entry shells, pre-filters measure entries by fingerprint
  and fast-paths the last measure entry. Direction inheritance is memoized per
  layout pass in `NodeData`, with an epoch invalidated by style/child changes.
- The JMH benchmark suite now builds trees once per trial and only clears caches
  per invocation (except the intentional build/destruction benchmarks), and
  includes the new Flex/Grid/Block/UI/relayout/lifecycle scenarios.

### P13 performance additions

- `TaffyTree.computeLayout` gained a tree-clean fast path: a request that repeats
  the last one (same root, same available space per axis, same rounding configuration
  and same default measure callbacks) on a tree whose root layout cache is still warm
  is answered from the stored layouts without running the layout algorithm or the
  rounding pass, so it costs O(1) instead of one visit per node. The gate is the
  existing dirty sentinel — every style, child-list or measure-callback change clears
  the layout cache of the changed node and of its ancestors — so invalidation
  semantics are unchanged. An installed `LayoutChangeListener` keeps the request on
  the normal path, because the listener contract reports every layout write. Explicit
  layout or cache writes (`setLayout`, `setUnroundedLayout`, `storeCacheEntry`),
  the rounding toggle, `clear()` and any layout pass started directly through
  `LayoutComputer` drop the recorded request.
- `hasNewLayout`/`needsVisit` still report a completed request for every node
  (through a tree-level layout generation, in O(1) rather than by marking the
  subtree), and `acknowledgeLayout`/`acknowledgeSubtree` still clear it. No layout
  value, no rounding result and no listener notification changes.
- Node storage is dense: `nodes`/`parents` are arrays indexed by `NodeId.value`
  (spilling to a map only when an id would push the array past 8x the live node
  count), so the per-node lookups in layout, rounding and dirty propagation skip
  the hash map. Public ids stay monotonic and are never reused; the remaining
  measure/context/grid side tables keep their maps.
- `roundLayout` compares the stored final layout field by field against the values
  the pass would compute and keeps it when they match, avoiding a `Layout` and
  geometry allocation per node; fractional layouts whose stored result is already
  stable get the same treatment. Block item lists are rebuilt only once per layout
  pass and are reused across the pass's measurement and final-layout calls; the
  list is also kept on `NodeData` across passes and invalidated by the container
  style/children versions and each child's style version, so changing one leaf no
  longer rebuilds every ancestor's item list.
- The final-layout cache entry now stores an exact packed key (three `long`s built
  with `floatToIntBits`/available-space type codes) instead of comparing six fields
  per lookup; `NaN`, `-0.0`, known-axis and parent-size semantics are unchanged.


### P7 additions (float, grid details, contain)

- CSS `float`/`clear` (style fields, `FloatContext`/`BlockContext`/`BfcSlot`),
  block-level float placement/avoidance/content slots, BFC float containment,
  root/as-float BFC semantics and the FT-02/FT-06 clear/inset/height fixes.
- Grid track sizing: fr intrinsic distribution (GR-01), minimum contribution
  completion (GR-02, including the spanning growth-limit distribution), container
  min/max content-box (GR-03), growth-limit headroom (GR-04), percentages against
  the grid area (GR-06), auto-fit/auto-repeat and sparse occupancy (GR-07/13),
  independent row rerun and percentage tracks (GR-11), cyclic percentage minima
  (GR-12), named lines/repeat line names (GR-15/17), sized grid-template-areas
  (GR-16), implicit-fixed stretch (GR-20) and additive `DetailedGridInfo`
  (GR-18).
- `contain: layout/paint` (independent formatting context and baseline
  suppression), `flex-wrap: balance/balance-reverse` with `flexLineCount` DP line
  breaking (FL-21).

### P6 additions (position / out-of-flow)

- `TaffyPosition` gained `STATIC`, `FIXED` and `STICKY` (appended; the 1.2 default
  stays `RELATIVE`, decision D1), plus `isOutOfFlow()`. Only `RELATIVE` applies
  relative insets; `FIXED` is out of flow.
- Out-of-flow boxes now honor `align-self`/`justify-self` (including safe
  overflow), auto margins only absorb space with both insets set, replaced boxes
  are not stretched by double insets, and the root is laid out in a
  viewport-sized initial containing block with RTL support. Flex absolute static
  positions are writing-mode relative and safe.

### P5 additions (alignment keywords)

- `NORMAL`, `SELF_START`/`SELF_END` and the `SAFE_*` overflow-position variants were
  appended to `AlignItems`, `AlignSelf`, `AlignContent` and `JustifyContent` (old
  ordinals unchanged; `AUTO` remains the "unset" sentinel). `AlignmentHelper`
  provides normal/self/safe resolution, fallbacks and offsets, wired into the block,
  flexbox and grid algorithms.
- P5 also adds block `justify-self` (BK-16), flex intrinsic `CONTENT_SIZE` sizing with
  the definite-preferred fast path (FL-18), on-demand transferred min/max sizes
  (FL-19) and column intrinsic cross clamping (FL-20).
- `SizingKeyword` (min-content/max-content/fit-content(calc)/stretch, `EXACT` vs
  `MEASURE`) is consumed by flex main/cross/flex-basis (ST-01/FL-17), and
  `height:stretch` treats collapsible block margins as zero (BK-19).

### P4 additions (scrollable overflow)

- `Layout.scrollableOverflow()` / `LayoutOutput.scrollableOverflow()` are now
  populated by the leaf, block, flex and grid algorithms; `RoundLayout` preserves
  and rounds the rectangle; `Layout.scrollWidth()`/`scrollHeight()` derive the
  reachable range from it, including reversed (RTL / `column-reverse` /
  `wrap-reverse`) scroll origins. Hand-built layouts keep the legacy
  `contentSize` formula as the 1.x compatibility path.
- New public `util.ScrollableOverflowUtil` (contribution/union/finalize) and the
  gentest SO-04 assertions for scroll containers' `scrollWidth`/`scrollHeight`.

### P3 additions (partial)

- `TaffyDisplay.FLOW_ROOT` (appended enum constant) with block-algorithm dispatch and
  the margin-isolation part of a new block formatting context. Float containment and
  float avoidance need the float layout work (F7) and are not implemented yet.
- `TaffyStyle.itemIsCompressibleReplaced`, `isCompressibleReplaced()` and
  `setCompressibleReplaced()`; `isCompressibleReplaced()` is
  `itemIsReplaced || itemIsCompressibleReplaced`, so the default is unchanged. The
  grid minimum-contribution cap now consults this getter. The #1351 second half
  (`%max` on form controls) and normal-alignment replaced behaviour are still TODO
  (T17/T4/S10).

### Fixed

- **PA-01 (grid)** — Absolutely positioned children now resolve named grid
  lines. The absolute-positioning path passed raw placements to the line
  resolver, which had no `NAMED_LINE` branch and fell back to the container
  edges, so abspos children using named lines filled the whole grid. The path
  now uses the same `NamedLineResolver` as in-flow placement.
- **PA-02 (flex)** — `align-self: start` and `align-self: end` are no longer
  folded into `flex-start`/`flex-end`. They resolve to the physical
  cross-axis start/end and keep that meaning under `flex-wrap: wrap-reverse`.
- **PA-03 (RTL)** — With a definite available width, the root node is placed
  at the inline end for RTL layout:
  `x = availableWidth - size.width - margin.right` instead of `0`.
- **PA-04 (style)** — `TaffyStyle.copy()` now copies the Yoga-style `flex`
  shorthand field, so a copied style keeps the effective flex-grow, flex-shrink,
  and flex-basis produced by `setFlex(...)`.
- **PA-06 (block)** — `display: none` descendants of block containers are laid
  out through the hidden path instead of being skipped. Their layout is reset to
  zero and the result is stored in the layout cache, so a hidden → visible
  toggle triggers a correct relayout instead of reusing stale visible values.
- **PA-05 (abspos)** — Content-sized absolutely positioned boxes now run a
  definite-final-size second pass. After the measure pass derives an auto axis,
  the child is laid out again with that definite final size, so percentage-sized
  descendants resolve against the final containing block. A `height: 50%`
  descendant of a 100px-tall content-sized abspos box now measures 50px instead
  of 0. The second pass only runs when an axis was content-derived and only
  invalidates that out-of-flow subtree's cache.
- **P1 batch (flex / block / grid)** — low-cost upstream fixes: flex intrinsic
  floors, scaled shrink factor, row/column clamping basis, negative-margin
  contributions, wrap constraints, unresolved-percentage flex basis, stretch /
  space-* fallbacks and zero floors; block measurement no longer writes layouts,
  run-mode propagation, ComputeSize margin outputs, percentage padding/margin
  bases, min-height margin collapse, own margins in `Layout.margin`, cumulative
  rounding, aspect-ratio height before child layout and abspos auto margins on
  the used size; grid track/line/span clamps, per-axis sizing caches, empty-axis
  content alignment, abspos track-edge resolution and bounded fr loops. In RTL,
  abspos `justify-self` now follows the writing direction. Generated
  expectations changed only where upstream regenerated them (BK-13).

### Source breaking

- `AlignSelf` gains `START` and `END`, appended after `STRETCH` (approved
  as a minor-version exception). The addition is binary compatible: existing
  constants keep their names, order, and ordinals. Downstream **exhaustive
  `switch` expressions** over `AlignSelf` (and exhaustive pattern switches)
  may fail to compile and need explicit `START`/`END` cases or a `default`
  branch; `switch` statements are unaffected. See
  [docs/migration-1.2.md](docs/migration-1.2.md).

### Behavior

- The fixes above change layout values only in scenarios that hit the defect.
  Every known changed value in the frozen snapshots is recorded in
  [compat-golden/ALLOWLIST.md](compat-golden/ALLOWLIST.md) (scenario, node path,
  field, old value, new value, `since`, `removeBy`), and
  [docs/migration-1.2.md](docs/migration-1.2.md) explains how to compare your
  output against it. Downstream pixel/snapshot tests may need updated
  expectations.
- **P7 batch (partial)**: float layout and clear, grid track sizing/named
  lines/detailed info, block `contain`, flex balance. Remaining/known residuals:
  TS-06's +646-fixture corpus, GR-10's `RequestedAxis` plumbing, float per-block
  intrinsic width, and the indefinite-axis limited min-content rule (no upstream
  fixture distinguishes it). ST-08 and OF-01 remain on the 1.3/2.0 line.
- **P6 batch (partial)**: `position: static/fixed/sticky` keywords, out-of-flow
  alignment/safe/margins, replaced sizing, root viewport ICB and RTL root, flex
  static positions. **OF-01** (full out-of-flow hoisting with an independent OOF
  pass and containing-block-based locations) is deferred to the 1.4 preview /
  2.0 default line (decision D2); 1.2 keeps parent-based out-of-flow layout.
- **P5 batch (partial)**: explicit `NORMAL` can override inherited alignment;
  `SELF_START`/`SELF_END` resolve against the item's own direction; `SAFE_*` falls
  back to start only on overflow. Block-level boxes honor `justify-self`; flex
  intrinsic main sizing uses the content size and transferred aspect-ratio
  min/max sizes are applied on demand. ST-01/FL-17/BK-19 (sizing-keyword parser,
  flex consumption, stretch-height margins) and ST-08 (min/max type change) remain
  deferred; FL-15/16 wait for the 2.0 `knownDimensionsAreDefinite` component.
- **P4 batch**: `scrollWidth()`/`scrollHeight()` now report the reachable range
  from the scrollable-overflow rectangle instead of `contentSize`; leaf
  `contentSize` is `start padding + measured + (scroll ? end padding)` (RTL
  swaps sides); block/flex/grid populate the rectangle for in-flow and absolute
  children.
- **P3 batch (partial)**: block children resolve percentage padding/inset and
  percentage heights against the content box (a definite height now provides the
  basis; `min-height` alone does not); margin collapse-through is decided by the
  used outer height; flex items subtract their margins from available space and
  auto-cross-margin items no longer participate in baseline alignment;
  `display: flow-root` no longer collapses its margins with its children;
  compressible replaced boxes cap their min-content width per #1401 (form-control
  `%max` no longer collapses to zero there); fixed-size leaves consult their
  measure callback for baselines; block containers apply `align-content`; grid
  baseline participation excludes auto block margins and cyclic block sizes, and
  the grid container baseline comes from the final layout without the relative
  offset.
- This batch does not change default layout options.

### API

- No public API removals. The additions above are source- and binary-compatible:
  the old 6/8-arg record constructors, `MeasureFunc`, and the old cache/measure
  methods remain. They will be deprecated as call sites migrate and removed in
  2.0.0, after a full 1.x deprecation window.
- Record schema change: `Layout` and `LayoutOutput` gain components, so their
  serialized JSON/`toString` shapes change (approved Q4=A; `RecordSchemaTest`
  pins component order). Old constructors still produce `ZERO`/absent values,
  so old writers keep working; old readers should be updated.
- `./gradlew checkApiCompatibility` stays green against the published `1.1.5`
  baseline; see [docs/api-compatibility.md](docs/api-compatibility.md).
- The maintainer-facing inventory of every source/binary/behavior break and
  deprecation is [docs/alignment-api-changes.md](docs/alignment-api-changes.md).
