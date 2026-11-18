# Upstream Alignment Baseline

This branch (`align/upstream-rust`) tracks the work of aligning taffy-java with
the upstream Rust [taffy](https://github.com/DioxusLabs/taffy) implementation.

## Baseline metadata

| Item | Value |
| --- | --- |
| Java baseline commit | `b594b52` (2026-09-27) |
| Upstream source baseline | `fd8f81fc` — "Mixed benchmark fixups (#900)", 2025-11-30 |
| Equivalent init-time snapshot | `175442d4` (2025-12-20; differs only in CI) |
| Upstream alignment target | `1abd8eea` (2026-11-13, after v0.15.1) |
| Upstream commit range | `fd8f81fc..1abd8eea` — 275 commits |
| Published API baseline | `dev.vfyjxf:taffy:1.1.5` (Maven Central) |

The source baseline was identified by matching the Java port against upstream
Rust commits (test names, style/layout/cache types, fixture categories); see
`Data/taffy-java/analysis/baseline-confirmation.md` in the workspace. The
"latest commit on Feb 19" (`6d8a3832`, README-only) is **not** the source
snapshot: it is 31 days after the initial Java commit and adds CSS float, which
the Java port never contained.

Commits between `fd8f81fc` and `6d8a3832` (4 commits, including float #866)
are part of the alignment scope. Commits before `fd8f81fc` are already part of
the ported snapshot; if Java lacks one of those features it is a port-time gap,
not a new upstream change.

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

The detailed, reviewed change list is produced from the commit ranges in
`Data/taffy-java/analysis/` and is not checked into this repository.
