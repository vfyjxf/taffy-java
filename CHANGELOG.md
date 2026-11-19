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
