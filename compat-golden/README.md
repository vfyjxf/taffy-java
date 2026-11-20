# Compatibility golden snapshots

Checked-in layout snapshots used to freeze the observable output of the P0-a
scenarios before the upstream-alignment fixes are merged.

## Provenance

- `p0a-baseline/` was captured at commit `49b7ef0` (`align/upstream-rust`).
- Project version at capture time: **1.1.4**.
- Published API compatibility baseline: **1.1.5**.
- The directory is deliberately named `p0a-baseline`, not after a release: these
  files are not shipped, and they record current behavior at a specific commit.

## Purpose and rules

Each file records the **current output**, not the "correct" output. Several P0-a
scenarios are known to be wrong on the baseline; their snapshots exist so that a
fix produces a small, reviewable diff instead of silent behavior drift.

- A P0-a fix branch **must not** edit `compat-golden/`. If a fix changes a
  snapshot, the branch stops and reports it; the lead updates the golden file
  after merging, together with the old/new values in the change allowlist.
- Updating a snapshot is a behavior change: review the JSON diff and confirm it
  matches the fix/commit that caused it.
- Snapshots must stay deterministic. Do not add timestamps, host names, JDK
  versions, iteration counts, or map ordering to the format.

## Cases (`p0a-baseline/`)

| File | Scenario | Known baseline issue captured |
| --- | --- | --- |
| `grid_named_lines_absolute_position.json` | Grid 200x150, padding 10, rows `[a] 20px [b] 50px [c]`, columns `[a] 40px [b] 60px [c]`, three absolutely positioned children using named lines | PA-01: named lines are not resolved on the abspos path; all three children currently fill the whole grid (200x150 at 0,0). |
| `flex_wrap_reverse_align_self.json` | Flex row, `wrap-reverse`, 100x100, `align-items:start`, five 40x20 children covering inherited/start/end/flex-start/flex-end `align-self` | PA-02: `AlignSelf.fromAlignItems` collapses START/END into FLEX_START/FLEX_END, so wrap-reverse positions are wrong. |
| `rtl_root_direction.json` | RTL root 200x100 with `margin-right:5` in a 300x150 viewport, two RTL flex children | PA-03: root location stays (0,0) instead of the RTL right edge (expected 95 = 300 - 200 - 5). |
| `style_copy_flex_shorthand.json` | Flex row, fixed 20px child plus a child whose style is `setFlex(2).copy()` | PA-04: `TaffyStyle.copy()` drops the `flex` shorthand, so the copied child currently has width 0 instead of growing to 80. |
| `abspos_content_sized_percentage_height.json` | Absolutely positioned block, width 100 / height auto, containing a 100x100 child and a `height:50%` child | PA-05: no definite-final-size second pass, so the percentage child currently gets height 0 instead of 50. |
| `block_display_none_descendants.json` | Block root; a subtree (plain child + abspos descendant) is laid out visible, switched to `display:none`, then laid out again | PA-06: the hidden-children loop is dead code, so the subtree keeps stale layouts (120x80 at y=30, etc.) instead of being cleared. |

The two-pass setup in the last case is intentional: hiding the subtree from the
start would only show the zero-initialized layouts, not the stale values that
PA-06 must clear.

## JSON format

```json
{
  "case": "style_copy_flex_shorthand",
  "description": "...",
  "availableSpace": { "width": 100.0000, "height": 100.0000 },
  "roundingEnabled": true,
  "nodes": [
    {
      "path": "root",
      "order": 0,
      "location": { "x": 0.0000, "y": 0.0000 },
      "size": { "width": 100.0000, "height": 100.0000 },
      "margin": { "left": 0.0000, "top": 0.0000, "right": 0.0000, "bottom": 0.0000 },
      "border": { "left": 0.0000, "top": 0.0000, "right": 0.0000, "bottom": 0.0000 },
      "padding": { "left": 0.0000, "top": 0.0000, "right": 0.0000, "bottom": 0.0000 },
      "contentSize": { "width": 0.0000, "height": 0.0000 },
      "scrollWidth": 0.0000,
      "scrollHeight": 0.0000
    }
  ]
}
```

Stability rules enforced by `GoldenSupport`:

- Nodes are a flat depth-first list. `path` is the tree path
  (`root`, `root/children[0]`, `root/children[1]/children[0]`, ...), so node
  identity does not depend on creation order.
- Keys are always emitted in the order shown above; `left/top/right/bottom`
  rect fields use that fixed order too.
- Floats are JSON numbers with exactly four decimal places, produced with
  `BigDecimal(Float.toString(value)).setScale(4, HALF_UP)`. There is no
  scientific notation, locale dependence, or `-0.0000`.
- UTF-8, `\n` line endings, two-space indentation, trailing newline.
- Layouts are read through `TaffyTree.getLayout` (rounded layouts while
  rounding is enabled) after `computeLayout`.

## Running

Assert (normal mode; missing or changed snapshots fail):

```bash
bash ./gradlew test --tests 'dev.vfyjxf.taffy.compat.*'
```

Rewrite all six snapshots (only after a fix is merged, and review the diff):

```bash
bash ./gradlew test --tests 'dev.vfyjxf.taffy.compat.*' -Dtaffy.compat.updateGolden=true
```

The root `build.gradle` test configuration forwards the
`taffy.compat.updateGolden` system property to the test JVM. On a mismatch the
test writes the actual JSON to `build/compat-golden-actual/<case>.json` and
reports the first differing line.

The snapshots are also exercised by the full suite (`bash ./gradlew test`).
