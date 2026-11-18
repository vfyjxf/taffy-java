# Migrating to 1.2.0

> Status: **unreleased**. This guide covers the 1.2.0 upstream-alignment batch
> (P0-a). The authoritative per-value record is
> [`compat-golden/ALLOWLIST.md`](../compat-golden/ALLOWLIST.md); this page explains
> how to use it. See also [CHANGELOG.md](../CHANGELOG.md) and
> [upstream-alignment.md](upstream-alignment.md).

## Who is affected

- Consumers upgrading from 1.1.x (the published API baseline is `1.1.5`) to
  1.2.0.
- Only layouts that hit one of the fixed defects change numbers. Trees that do
  not exercise those scenarios produce identical output.
- Downstream code with an **exhaustive `switch` expression over `AlignSelf`**
  can fail to compile even when its layouts do not change.

The fixes in this batch:

| ID | Area | Summary |
| --- | --- | --- |
| PA-01 | grid | Absolutely positioned children resolve named grid lines. |
| PA-02 | flex | `align-self: start`/`end` under `flex-wrap: wrap-reverse`. |
| PA-03 | RTL | Root node is placed at the inline end (definite available width). |
| PA-04 | style | `TaffyStyle.copy()` keeps the `flex` shorthand. |
| PA-06 | block | `display: none` descendants are cleared instead of keeping stale layouts. |
| PA-05 | abspos | **Pending**: merges after this guide; its row will be added to the allowlist. |

## How to compare your output with the allowlist

The allowlist is a table in which one row describes one changed field group in
one frozen scenario; a row may cover several nodes when they change together.
The columns are:

- `场景` (scenario) — the case name, matching
  `compat-golden/p0a-baseline/<scenario>.json`.
- `路径` (path) — `nodes/<n>`, the zero-based index into that case's `nodes`
  array. The JSON also carries stable tree paths (`root/children[0]/...`);
  [`compat-golden/README.md`](../compat-golden/README.md) documents the format.
- `字段` (field) — the layout field that changes, such as `location.x`,
  `size.height`, `contentSize.width`, or `order`.
- `old` / `new` — the value before and after the fix, in the snapshot's units
  (four decimal places).
- `修复` (fix) — the PA item that caused the change.
- `原因` (reason) — a short explanation of the defect.
- `since` — the first version that contains the change (`1.2.0`).
- `removeBy` — when the entry may be deleted; bug-fix entries are permanent
  ("不删除").

Procedure:

1. Upgrade and re-run your layout tests or snapshot suite.
2. For each difference, find the row matching scenario, node index, and field.
3. A matching row means the difference is expected: update your expected values
   or snapshot baselines.
4. No matching row, or a different value for the same field, means an
   unexpected difference: report it (see below).

Notes:

- The allowlist enumerates the known changes in the frozen scenarios. The same
  root cause can change analogous trees in your application in the same
  direction; those changes are expected even when no dedicated row exists.
  Compare the node, field, and direction of the change rather than a single
  scalar in isolation.
- `compat-golden/` is test infrastructure and is not shipped. Do not use the
  baseline JSON as the 1.2.0 expected output; the allowlist is the old/new
  record.
- Layout rounding and default layout options are unchanged by this batch. All
  allowlist values are rounded to four decimal places.

## Downstream `AlignSelf` switches

`AlignSelf` gains two constants appended after `STRETCH`: `START` and
`END` (approved minor-version source-compatibility exception). Existing
constant names, order, and ordinals are unchanged, and the addition is binary
compatible.

`switch` statements keep compiling unchanged. **Exhaustive `switch`
expressions** (and exhaustive pattern switches) must handle the new constants,
either with explicit cases or with a `default` branch:

```java
// 1.1.x: exhaustive over the six old constants
AlignSelf resolved = switch (self) {
    case AUTO, STRETCH -> AlignSelf.STRETCH;
    case FLEX_START, FLEX_END, CENTER, BASELINE -> self;
};

// 1.2.0: add the new cases (or a default branch)
AlignSelf resolved = switch (self) {
    case AUTO, STRETCH -> AlignSelf.STRETCH;
    case START, END, FLEX_START, FLEX_END, CENTER, BASELINE -> self;
};
```

Semantics reminder: `START` and `END` are the physical cross-axis edges and
are not flipped by `flex-wrap: wrap-reverse`; `FLEX_START` and `FLEX_END`
follow the flex direction. If you persist enum values, persist names
(`START`, `END`), not `ordinal()`.

## API compatibility

This batch contains no public API removals, field/type changes, or method
signature changes. `./gradlew checkApiCompatibility` remains green against the
published `1.1.5` baseline; see [api-compatibility.md](api-compatibility.md).
Deprecations begin in the following batch (P0-b) and are kept until 2.0.0; they
are not part of 1.2.0.

## Reporting a difference

If a layout difference is unexpected — no allowlist row, or a different value for
the same field — report it at
<https://github.com/vfyjxf/taffy-java/issues> with:

- taffy-java version or commit, and JDK version;
- the minimal tree (styles and structure) and the `computeLayout` available
  space;
- the affected node path, field, the expected value from the allowlist (or
  "not listed"), and the actual value;
- whether layout rounding was enabled;
- the matching `p0a-baseline` scenario name, if any.

A runnable snippet or a JUnit test is the fastest way to confirm the cause.
