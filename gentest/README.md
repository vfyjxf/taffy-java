# gentest

Generates JUnit tests for **taffy-java** by using headless Chrome as a layout
oracle. Two input sources share one pipeline:

| Source | Input | Output package (default) |
| --- | --- | --- |
| `taffy` | Rust taffy `test_fixtures/<category>/*.html` | `dev.vfyjxf.taffy.generated.<category>` |
| `wpt` | web-platform-tests HTML (CSS grid/flex/aspect-ratio suites) | `dev.vfyjxf.taffy.wpt.<suite>` |

Both sources use the same fixture discovery, style-to-Java emitter, test naming,
class scaffolding and Chrome oracle. Source adapters only differ in how they
discover files, map categories to packages, and extract layout/style data.

## Architecture

| Class | Responsibility |
| --- | --- |
| `Generator` | Single CLI entry point, `--check` comparison, dry-run |
| `GeneratorOptions` | Unified option parser + legacy aliases |
| `FreshnessAllowlist` | Metadata-carrying method-level exceptions for `--check` |
| `FixtureDiscovery` | Deterministic HTML discovery and filters |
| `StyleCodegen` | `NodeStyle` model, complete `TaffyStyle` mapping, Java emission |
| `JavaTestWriter` | Class headers, naming, escaping, UTF-8/LF output |
| `ChromeOracle` | Browser/driver resolution, headless Chrome, scrollbar probe |
| `LocalHttpServer` | Serves a WPT checkout for pages with absolute resource URLs |
| `TaffySource` | Discover `test_fixtures/`, drive Chrome, emit categories |
| `WptSource` | Discover suites, collect DOM, map CSS, emit chunked classes |
| `WptDomCollector` | Shared DOM/geometry JavaScript |
| `WptSupport` | Feature-based supported/skipped classification + skip statistics |
| `WptToFixtureConverter` | WPT HTML -> taffy fixture conversion |

## Requirements

* JDK 21 (Gradle toolchain).
* Google Chrome / Chromium.
* Network access on first run, or an explicit chromedriver, because the driver
  is resolved by Selenium Manager. `--chrome-driver` bypasses it and
  WebDriverManager is used as a fallback.

Chrome binary resolution order:

1. `--chrome-binary <path>`
2. `TAFFY_CHROME_BINARY`, `CHROME_BIN`, `CHROME_PATH`
3. Common macOS / Windows / Linux installations
4. Selenium Manager discovery

chromedriver resolution: `--chrome-driver` > Selenium Manager > WebDriverManager fallback.

The generator adds `--disable-features=OverlayScrollbar` so
`test_base_style.css`'s 15px scrollbars actually occupy space in modern
headless Chrome (without it, Chrome 120+ reports 0 and layout values drift).

## Quick start

From the repository root:

```bash
# taffy fixtures (all categories from ../taffy)
./gradlew :gentest:genTaffy -PtaffyRoot=/path/to/taffy

# one category
./gradlew :gentest:genTaffy -PtaffyRoot=/path/to/taffy -Pcategory=flex

# WPT checkout (sparse checkout of css/ is enough)
./gradlew :gentest:genWpt -PwptRoot=/path/to/wpt

# both
./gradlew :gentest:genAll -PtaffyRoot=/path/to/taffy -PwptRoot=/path/to/wpt

# verify committed generated tests without modifying them
./gradlew :gentest:checkGenerated -PtaffyRoot=/path/to/taffy
./gradlew :gentest:checkAll -PtaffyRoot=/path/to/taffy -PwptRoot=/path/to/wpt

# fail when the committed generated tests are stale (opt-in; needs Chrome)
./gradlew :gentest:checkGeneratedFreshness \
  -Ptaffy.checkGenerated=true -PtaffyRoot=/path/to/taffy
# or attach the same gate to the normal lifecycle of every project
./gradlew check -Ptaffy.checkGenerated=true -PtaffyRoot=/path/to/taffy
```

The unified CLI can also be called directly:

```bash
./gradlew :gentest:installDist
./gentest/build/install/gentest/bin/gentest --source taffy \
  --taffy-root /path/to/taffy --category leaf --out src/test/java
```

## CLI

```
Generator --source taffy|wpt|all [options]

--taffy-root <dir>       Rust taffy checkout (contains test_fixtures/ and scripts/)
                         default: ../taffy when it exists, then TAFFY_ROOT
--wpt-root <dir>         WPT checkout root (contains css/)
                         default: ../wpt when it exists, then WPT_ROOT
--wpt-css-root <dir>     Directory that contains css/ for split checkouts
--wpt-suites <csv>       default: css/css-grid,css/css-flexbox,css/css-sizing/aspect-ratio

--out <dir>              Java source root (default: src/test/java)
--package <name>         base package (taffy: dev.vfyjxf.taffy.generated,
                         WPT: dev.vfyjxf.taffy.wpt)
--category <csv>         taffy category and/or WPT suite short name / path prefix
--filter <glob[,glob]>   match root-relative path or fixture name
--filter-file <file>     one root-relative path per line (# comments allowed)

--chrome-binary <path>   override browser detection
--chrome-driver <path>   override driver detection
--check                  generate into a temp dir and byte-compare with --out
--freshness-allowlist <file>
                         --check: accepted hand-edited methods with metadata
--dry-run                discover and print planned files, do not launch Chrome
```

Exit codes: `0` success, `1` generation/check failure, `2` usage error.

### Gradle properties / environment variables

| Property | Environment | Purpose |
| --- | --- | --- |
| `-PtaffyRoot` | `TAFFY_ROOT` | Rust taffy checkout |
| `-PwptRoot` | `WPT_ROOT` | WPT checkout |
| `-PwptCssRoot` | `WPT_CSS_ROOT` | directory containing `css/` |
| `-PoutRoot` | `GEN_OUT_ROOT` | Java source root |
| `-Ptaffy.checkGenerated` | `TAFFY_CHECK_GENERATED` | opt-in generated-test freshness gate |
| `-Pcategory` `-Pfilter` `-PfilterFile` | - | generation filters |
| `-PwptSuites` | - | WPT suites |
| `-PchromeBinary` | `TAFFY_CHROME_BINARY`, `CHROME_BIN`, `CHROME_PATH` | Chrome |
| `-PchromeDriver` | `TAFFY_CHROME_DRIVER`, `CHROMEDRIVER` | chromedriver |
| `-PconvertInput` `-PconvertOutput` `-PconvertPrefix` `-PconvertPattern` | `CONVERT_INPUT`, `CONVERT_OUTPUT` | WPT fixture conversion |

Missing directories produce an error that names the property/environment
variable to set.

## WPT conversion to taffy fixtures

Large parts of the old WPT generator can still be exercised through the taffy
pipeline by converting WPT HTML into taffy fixture format:

```bash
./gradlew :gentest:convertWptToFixtures \
  -PwptRoot=/path/to/wpt \
  -PtaffyRoot=/path/to/taffy \
  -PconvertInput=/path/to/wpt/css/css-sizing/aspect-ratio \
  -PconvertOutput=/path/to/taffy/test_fixtures/flex_aspect_ratio

./gradlew :gentest:genTaffy -PtaffyRoot=/path/to/taffy -Pcategory=flex_aspect_ratio
```

Equivalent CLI:

```bash
Generator --source wpt --convert-to-fixtures \
  --convert-input /path/to/wpt/css/css-sizing/aspect-ratio \
  --convert-output /path/to/taffy/test_fixtures/flex_aspect_ratio \
  --prefix wpt_ --pattern 'flex-aspect-ratio-*.html'
```

Reference/manual files (`-ref.html`, `-notref.html`, `-manual`) and files with
scripts or unsupported replaced elements are skipped.

## Determinism and --check

* Fixtures are sorted by category then root-relative path.
* Output uses UTF-8, LF line endings and fixed indentation.
* Float formatting is locale-independent.
* `--check` always writes to a fresh temp directory; it never modifies the
  committed tree. Every generated file is compared byte for byte, and stale
  files in the generated category directories are reported too.

Existing committed tests can be reproduced with the Rust taffy revision that
pre-dates the direction-support helper rename (`60601a65^`):

```bash
git -C /path/to/taffy archive 60601a65^ test_fixtures scripts | tar -x -C /tmp/taffy-2026-03
./gradlew :gentest:checkGenerated -PtaffyRoot=/tmp/taffy-2026-03 \
  -Pcategory=leaf -PchromeBinary='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
```

This revision is the last one whose fixtures match the committed
`Generated/**/*Test.java` files (537 non-disabled flex fixtures, 14 leaf
fixtures, legacy `borderBoxData/contentBoxData` helper schema).

## CI: generated-test freshness gate

`:gentest:checkGeneratedFreshness` fails when the committed generated tests
no longer match what the generator emits from the configured taffy checkout. It
reuses `:gentest:checkGenerated` (single generation and comparison path) and
adds an opt-in preflight plus lifecycle wiring.

The gate is **opt-in**: it needs a Rust taffy checkout and Chrome, so
`check`/`build` never run it by default. Invoking the task without
opting in skips it and prints how to enable it instead of silently passing.

```bash
# direct
./gradlew :gentest:checkGeneratedFreshness \
  -Ptaffy.checkGenerated=true \
  -PtaffyRoot=/path/to/taffy \
  -PchromeBinary='/path/to/chrome' \
  -PchromeDriver=/path/to/chromedriver

# attached to :gentest:check (and therefore ./gradlew check / ./gradlew build)
./gradlew check -Ptaffy.checkGenerated=true -PtaffyRoot=/path/to/taffy
```

| Property | Environment | Purpose |
| --- | --- | --- |
| `-Ptaffy.checkGenerated=true` | `TAFFY_CHECK_GENERATED=true` | enable the gate and attach it to `:gentest:check` |

### CI requirements

* JDK 21 (Gradle toolchain) for `:gentest`.
* Chrome/Chromium. The preflight rejects a missing binary and names the
  `-PchromeBinary` / `TAFFY_CHROME_BINARY` alternatives.
* chromedriver: pass `-PchromeDriver` (or `TAFFY_CHROME_DRIVER` /
  `CHROMEDRIVER`), or allow Selenium Manager to resolve and download a
  matching driver on first use (network access).
* A taffy checkout with `test_fixtures/` and `scripts/` via
  `-PtaffyRoot`/`TAFFY_ROOT` (or the default `../taffy`).

For a hermetic/offline job: bake Chrome and a matching chromedriver into the
image, pass both paths explicitly, warm the Gradle dependency cache, then add
`--offline`. With both paths supplied, generation itself does not need
network access; otherwise Selenium Manager downloads the driver on first run.

### Historical fixture tree

The committed tests correspond to `60601a65^` (see *Determinism and --check*
above):

```bash
git -C /path/to/taffy archive '60601a65^' test_fixtures scripts | tar -x -C /tmp/taffy-2026-03
./gradlew :gentest:checkGeneratedFreshness \
  -Ptaffy.checkGenerated=true -PtaffyRoot=/tmp/taffy-2026-03 \
  -PchromeBinary='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
```

Expected result: `CHECK PASSED: 7 file(s) byte-identical` followed by
`(2 allowlisted hand edit(s) skipped)`, with two
`allowlisted hand edit skipped:` lines.

### Allowlist for accepted hand edits

`gentest/freshness-allowlist.txt` names individual generated test methods
that intentionally deviate from regeneration. It applies to the taffy source
only (`--freshness-allowlist` is rejected with `--source wpt`). Whole-file
exclusions and silent skips are not supported:

* every hit prints
  `allowlisted hand edit skipped: <file> :: <@DisplayName> (<reason>)`;
* an entry whose method already matches regeneration, or whose file/method is
  missing from the compared tree, is reported as **stale** and fails the check;
* an entry whose file exists in the committed tree but is outside the compared
  category subset (for example `-Pcategory=leaf`) is reported as info and not
  evaluated;
* every non-allowlisted difference still fails.

Each entry is one line with five `|`-separated fields (no `|` inside a
field):

```text
<relative path under the generated root> | <@DisplayName or method> | reason=<why> | since=<when/where> | removeBy=<when to delete>
```

The current entries keep the two `align_baseline_child_margin_percent__*`
methods of `flex/FlexTest.java` at the BK-13 rounded expectation (`y=0`)
because the historical fixture helper still measures `y=1` (upstream #1189
regenerated the fixture expectation to `y=0`). Remove an entry in the change
that regenerates the category from a tree at or after #1189; a stale entry fails
the gate, so it cannot silently outlive its reason.

### Regenerating after a failure

```text
CHECK FAILED: differs: <committed file>
  line N expected: <committed line>
  line N actual  : <freshly generated line>
...
CHECK FAILED: 1 difference(s) across 7 compared file(s)
Refresh with: ./gradlew :gentest:genTaffy -PtaffyRoot=<taffy checkout> -Pcategory=flex and commit the regenerated files (never edit generated tests by hand).
```

The `-Pcategory` list is derived from the differing files. Regenerate only
those categories, review the changed generated tests, and commit them with the
fixture/generator change that caused the difference. Files compared through the
allowlist use `CHECK FAILED: differs (after allowlisted methods):` as the
header.

`checkGeneratedFreshness` stays taffy-only. WPT output is committed under
`src/test/java/dev/vfyjxf/taffy/wpt/` and is verified by the separate explicit
`checkWpt` task (regenerate into a temp tree, compare byte for byte).

## Legacy entry points

* `TestGenerator` delegates to `Generator`.
* `WptTestGenerator` translates to `Generator --source wpt ...`.
* `WptToFixtureConverter <dir> <dir> [--prefix] [--pattern]` still accepts the
  old positional CLI.
* Old option spellings (`--chromeBinary`, `--chromeDriver`, `--category`,
  `--outRoot`, `--wptRoot`, `--cssRoot`, `--allowlist`, `--discover`,
  `--suites`, `--maxFiles`, `--acceptedOut`) are accepted; a deprecation
  notice is printed. Prefer the kebab-case options above.

## Verification

Commands used for the refactor (macOS, Chrome 157, JDK 21):

```bash
bash ./gradlew :gentest:build
bash ./gradlew :gentest:checkGenerated -PtaffyRoot=/tmp/taffy-2026-03 \
  -PchromeBinary='/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'
```

Results:

* `:gentest:build` passes.
* Default `checkGenerated` checks every committed taffy category present in the
  checkout; with the historical fixture tree and `gentest/freshness-allowlist.txt`
  it reports **7 files byte-identical, 1030 fixtures** (`block`, `blockflex`,
  `blockgrid`, `flex`, `grid`, `gridflex`, `leaf`) plus **2 allowlisted
  hand edits skipped** (BK-13, see the CI section).
* `:gentest:checkGeneratedFreshness` passes on the same tree; editing a
  third, non-allowlisted generated method fails the gate with the file and first
  differing line, and making an allowlisted method match regeneration fails it as
  a stale entry.

### Generator contract tests

`:gentest:test` runs pure JUnit tests (no Chrome required) for:

* the 15px scrollbar contract: the probe is asserted against 15px, and
  `-Dtaffy.gentest.expectedScrollbarWidth=<px>` is the explicit opt-in for
  platforms that genuinely use another width. A mismatch aborts generation
  instead of writing a suite with different assumptions.
* page readiness: navigation waits for the load event, the base stylesheet is
  probed before `getTestData()` switches body classes, and transient
  `getTestData()` failures retry.
* the `#1138` grid-template contract: CSS strings (modern helper) and legacy
  JSON arrays both map to the same Java style model.
* generated-tree freshness: identical trees, changed bytes (file and first
  differing line reported), missing/stale committed files, empty generated
  trees, method-level allowlist hits, stale allowlist entries, and the allowlist
  metadata parser.
* `flex_aspect_ratio`, `intrinsic` and `rtl` are committed but their fixture
  sources are not part of upstream taffy; they cannot be byte-checked from a
  pristine upstream checkout.
* Real sparse WPT checkout (`css/css-sizing/aspect-ratio`, 300 fixtures):
  **191 tests generated across 10 chunked classes**, 109 fixtures skipped with a
  per-file reason (see the WPT support policy below); `checkWpt` reports
  `CHECK PASSED: 10 file(s) byte-identical`.
* At the time this output was committed, 58 of the 191 tests still failed on
  core aspect-ratio semantics (intrinsic contributions, flex/grid stretch,
  0/infinity guards, abspos auto sizing). Those are tracked by the core layout
  workstream; the generator no longer emits tests for unsupported features.
* WPT-converted flex-aspect-ratio fixtures: 43 fixtures. New versus historical
  generator output differs only by 8 `flexBasis = TaffyDimension.content()`
  lines (the bug fix below).

`checkGenerated` is taffy-only and only checks categories that already have
committed output. **WPT output is not committed and is deliberately not part of
`:gentest:build` or the default check gate**; use `checkWpt`/`checkAll`
explicitly once such output is checked in.

## WPT support policy (skipped fixtures)

The WPT adapter only emits tests for pages whose layout semantics taffy-java
actually implements. Every other fixture is skipped with a per-file log line

```text
[skip] <root-relative path>: <reason>
```

and a per-reason summary at the end of the run:

```text
WPT skips: 109 fixture(s)
  [skip] 28x inline-level test container requires a line box (display: inline)
  ...
WPT accepted: 191 test(s) from 300 fixture(s)
```

Skips are decided from immutable page features, never from assertion outcomes:

| Reason | Why it is not modelled |
| --- | --- |
| table layout (`<table>`, `<td>`, `display: table`) | no table formatting implementation |
| UA form controls (`<fieldset>`, `<select>`, `<input>`, `<button>`, ...) | UA intrinsic sizing / widget layout are browser-specific |
| embedded content (`<svg>`, `<object>`, `<embed>`, `<iframe>`, `<audio>`, `<video>`) | intrinsic sizes and ratios are UA-defined and not collected |
| vertical `writing-mode` | no writing-mode support; the axes would be transposed |
| `contain: size` / `contain-intrinsic-size` | size containment has no layout effect in the engine |
| multi-column (`columns`, `column-count`) | no fragmentation support |
| replaced resource failed to load (`naturalWidth == 0`) | the browser falls back to alt text; the intrinsic size is unknown |
| inline-level test container | a line box decides its x/y; inline formatting is not modelled |
| inline-level/text content inside a block container | same line-box limitation (inline children of flex/grid containers are blockified and stay supported) |
| test element is `<body>` / no layout test element | the generated root is the viewport-sized body; script-only or body-styles pages carry no layout tree |

Mapped style features and known approximations:

* `float` / `clear` -> `TaffyFloat` / `Clear` (`inline-start`/`inline-end`
  resolve through `direction`).
* `overflow` / `overflow-x` / `overflow-y` -> `Overflow`. CSS `auto` maps to
  `Overflow.HIDDEN`: the engine's `SCROLL` always reserves a scrollbar gutter,
  while `auto` without overflow reserves none.
* `display: flow-root` -> `TaffyDisplay.FLOW_ROOT` (independent formatting
  context / float containment).
* Replaced elements (`<img>`, `<canvas>`): `itemIsReplaced = true` (consumed by
  block/flex/grid stretch and minimum contributions), the natural ratio replaces
  `aspect-ratio: auto ...`, and a `replacedIntrinsicSize` measure callback
  supplies the natural size when both axes are auto.
* Logical `inline-size`/`block-size` keep their computed physical size instead of
  being overwritten with `auto` by the aspect-ratio normalisation.
* `aspect-ratio: auto <ratio>` uses the natural ratio when the resource loaded
  and the declared ratio otherwise.
* Generated classes hold up to 20 *accepted* tests; chunking counts accepted
  tests, not discovered fixtures, so a skipped fixture no longer leaves holes.

## Known limitations

* **Modern taffy helper schema**: current taffy reports
  `borderBoxLtrData/contentBoxLtrData/...` and four direction variants. The
  generator reads the LTR variants and emits the two legacy test names
  (`__border_box`, `__content_box`); RTL variant generation is not implemented
  yet. Grid templates reported as CSS strings are parsed by the shared CSS
  parser (named lines are dropped).
* **Committed byte-check requires the historical fixture tree**; current
  upstream fixtures include one added leaf fixture and newer helper semantics,
  so a full `--check` against current upstream will report differences. Two
  BK-13 expectations are accepted through `gentest/freshness-allowlist.txt`;
  the gate fails when such an entry becomes stale.
* **WPT direct tests are committed** under `src/test/java/dev/vfyjxf/taffy/wpt/`.
  The adapter only emits pages whose features taffy-java models and logs every
  skipped fixture with a reason (see "WPT support policy" above); residual test
  failures belong to core aspect-ratio semantics, not to the generator.
* **Flex-aspect-ratio drift**: current WPT declares `flex-basis: content` on
  `flex-aspect-ratio-019..022`, while the committed
  `flex_aspect_ratio/FlexAspectRatioTest.java` contains that assignment only
  for 022. The new generator converts all four consistently; a byte-check
  against current WPT therefore reports those four fixtures.
* **Named grid lines / subgrid / replaced elements** are not representable in
  the generated `TaffyStyle` subset; such WPT inputs are skipped or parsed
  without the named lines.
* Selenium Manager must download a matching driver on the first run unless
  `--chrome-driver` is supplied.

## Bug fixes folded into the refactor

* Chrome scrollbar measurement now uses a sized overflowing probe and disables
  overlay scrollbars, matching the historical 15px layout on modern Chrome.
* Legacy helper JSON without `direction` defaults to the computed LTR value,
  preserving the committed generated output.
* `flex-basis: content` now maps to `TaffyDimension.content()` in the taffy
  fixture path (was silently dropped).
* Nested taffy fixture directories are mapped to dotted packages instead of
  producing invalid Java packages.
* Auto-repeat on `grid-auto-columns/rows` now fails with a clear error instead
  of emitting a non-existent `*WithRepeat` field.
* WPT `justify-content`, `align-self`, `justify-self` use the correct
  `AlignContent`/`AlignItems` field types in split-method mode too.
* WPT reference/manual files are skipped during fixture conversion.
