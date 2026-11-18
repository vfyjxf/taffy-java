# Changelog

All notable changes to taffy-java are documented in this file.

## 1.2.0 (unreleased)

First batch of the upstream-alignment work (P0-a): behavior fixes for grid,
flex, RTL roots, style copying, and block hidden descendants. Public API
signatures are unchanged, but layouts that hit the fixed defects change. The
per-value record is [compat-golden/ALLOWLIST.md](compat-golden/ALLOWLIST.md);
the upgrade guide is [docs/migration-1.2.md](docs/migration-1.2.md).

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
- This batch does not change default layout options.

### API

- No public API removals, field/type changes, or method signature changes.
  `./gradlew checkApiCompatibility` stays green against the published `1.1.5`
  baseline; see [docs/api-compatibility.md](docs/api-compatibility.md).
- Deprecation of the old API starts in the next batch (P0-b), not in 1.2.0; the
  deprecated API is kept until 2.0.0.
