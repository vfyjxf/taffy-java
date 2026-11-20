# Upstream Alignment Baseline

This branch (`align/upstream-rust`) tracks the work of aligning taffy-java with
the upstream Rust [taffy](https://github.com/DioxusLabs/taffy) implementation.

## Baseline metadata

| Item | Value |
| --- | --- |
| Java baseline commit | `b594b52` (2026-09-27) |
| Upstream port baseline (maintainer) | `6d8a3832` (2026-02-19) |
| Code-fingerprint snapshot | `fd8f81fc` (2025-11-30; 2,060 test names and core types match) |
| Upstream alignment target | `1abd8eea` (2026-11-13, after v0.15.1) |
| Alignment commit range | `6d8a3832..1abd8eea` — 271 commits |
| Working superset range | `fd8f81fc..1abd8eea` — 275 commits (4 pre-baseline commits kept as port-time gaps) |
| Published API baseline | `dev.vfyjxf:taffy:1.1.5` (Maven Central) |

The port baseline is recorded as `6d8a3832` (2026-02-19) per the maintainer.
The Java code fingerprint (2,060 generated test names and core style/layout/
cache types) matches `fd8f81fc` (2025-11-30); see
`Data/taffy-java/analysis/baseline-confirmation.md` in the workspace. The four
commits between `fd8f81fc` and `6d8a3832` — including CSS float `#866` — are
therefore treated as port-time gaps, and the work list keeps the 275-commit
superset so nothing is lost. Commits before `fd8f81fc` are part of the ported
snapshot.

## Current setup state

- Gradle wrapper: 9.9.1
- API compatibility: `./gradlew checkApiCompatibility` compares the plain jar
  against the published 1.1.5 artifact (JarCompatibilityChecker, see
  `docs/api-compatibility.md`).
- Tests: `./gradlew check` runs the full suite plus the API check.

## Alignment workflow

1. Keep the Java public API compatible: every batch must keep
   `checkApiCompatibility` green unless a breaking change is explicitly
   approved.
2. Port upstream behaviour in reviewable batches; add or regenerate fixtures
   for each batch.
3. Keep the Java-specific additions (incremental layout, change listeners,
   direction/RTL handling, convenience API) intact while porting.

## Alignment progress (1.2.0)

- **P0-a (done, tag `p0a-complete`)**: six existing Java defects fixed (grid
  named lines for abspos, AlignSelf START/END under wrap-reverse, RTL root,
  `copy()` flex shorthand, definite-final-size second pass for abspos,
  block `display:none` descendants), with a frozen behavior golden and a
  per-value allowlist in `compat-golden/ALLOWLIST.md`.
- **P0-b (done, tag `p0b-complete`)**: cache keys now include parent size and
  the cache-wide sizing/margin mode (with second-chance eviction and a
  `remove` parent invalidation); `ScrollableOverflow` and `Baselines` record
  components were added alongside the legacy ones; `MeasureFunction` (with
  `MeasureContext`) is the new measure contract and the old `MeasureFunc` is
  adapted; the S5 intrinsic-keyword helper is present but not wired
  (LEGACY_AUTO).
- **Deferred**: B-01 (`LayoutInput.knownDimensionsAreDefinite`) needs a record
  component, so it is part of the 2.0 component change; 1.x measure cache keys
  conservatively compare the full parent size instead.
- **P1 (done, tag `p1-complete`)**: low-cost upstream fixes across flex, block
  and grid (including abspos track edges and empty-axis alignment), plus the
  gentest scrollbar/readiness/contract work. Remaining partials are tracked in
  the workspace analysis (grid rerun container size, BK-15's height-only
  aspect-ratio derivation, BK-04/BL-07 dependency).
- **P2 (done, tag `p2-complete`)**: `writeTree`/transpose helpers and the
  opt-in generated-test freshness gate (with a method-level allowlist for the
  two BK-13 hand edits).
- **P3 (done, tag `p3-complete`)**: ST-05 replaced split, BK-08/09/12/14/17,
  FL-10/11/12/13/14 and the CR-07/BL-06 baseline helper + block/flex/grid
  propagation are merged; `display: flow-root` currently implements the
  margin-isolation part (float containment awaits F7). Known partials: BK-15's
  height-only aspect-ratio derivation and BK-04's block stretch exclusion
  (tracked in the workspace residuals).
- **P7 (partial, tag `p7-complete`)**: float/clear engine (placement, avoidance,
  BFC contexts, FT-02/06 fixes), grid track sizing (GR-01/02/03/04/06/07/11/12/13/
  15/16/17/20), additive `DetailedGridInfo` (GR-18 first/second slices), block
  `contain: layout/paint` (ST-07), and flex `balance`/`lineCount` (FL-21).
  Remaining: TS-06's expanded fixture corpus, GR-10 `RequestedAxis` plumbing,
  and the indefinite-axis limited min-content rule (no distinguishing upstream
  fixture). ST-08/OF-01 stay on the 1.3/2.0 line.
- **P6 (partial, tag `p6-complete`)**: OF-02/03/04/05/06/07, ST-06 and FL-22/23
  are merged (position keywords, abspos margins/replaced/self/safe, root
  viewport ICB/RTL, flex writing-mode static positions). **OF-01** (full OOF
  hoisting + containing-block-based locations) is deferred to the 1.4 preview /
  2.0 default (D2), so 1.2 still lays out out-of-flow boxes from the direct
  parent.
- **P5 (done, tag `p5-complete`)**: ST-02/ST-03/ST-04 keyword model +
  `AlignmentHelper`, BK-14/BK-16, FL-18/19/20, ST-01/FL-17 (`SizingKeyword`) and
  BK-19 are merged across block/flex/grid. Deferred by dependency/decision:
  ST-08 (min/max type change, S5 → 1.3/2.0) and FL-15/16 (need the 2.0
  `knownDimensionsAreDefinite` component).
- **P4 (done, tag `p4-complete`)**: leaf/block/flex/grid populate
  `ScrollableOverflow`; `scrollWidth()`/`scrollHeight()` derive the reachable
  range from it (including reversed origins); `ScrollableOverflowUtil` shared by
  all three algorithms; SO-04 gentest assertions are regenerated from the frozen
  `60601a65^` fixture tree and `:gentest:checkGenerated` passes (2 BK-13 allowlisted
  hand edits). The legacy `contentSize`
  component remains until 2.0 and keeps its 1.x RTL divergence for flex/grid.
- **Next**: P5 (sizing keywords, safe/self alignment) then P6/P7.

The detailed, reviewed change list is produced from the commit ranges in
`Data/taffy-java/analysis/` and is not checked into this repository.
