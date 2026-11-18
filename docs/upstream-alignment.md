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
- **Next**: P2 (cache/tree formalization) and P3 (block semantics/baselines)
  per the reviewed work list.

The detailed, reviewed change list is produced from the commit ranges in
`Data/taffy-java/analysis/` and is not checked into this repository.
