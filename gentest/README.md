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
that intentionally deviate from regeneration. Whole-file exclusions and silent
skips are not supported:

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

`checkGeneratedFreshness` is taffy-only. WPT output is not committed, so
`checkWpt` remains a separate explicit task.

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
* WPT self-contained sample: `genWpt` + `checkWpt` pass (1 file byte-identical).
* Real sparse WPT checkout (`css/css-sizing/aspect-ratio`, 60 files): 300 tests
  generated across 15 chunked classes, all compile with `javac`; a second run
  is byte-identical.
* WPT-converted flex-aspect-ratio fixtures: 43 fixtures. New versus historical
  generator output differs only by 8 `flexBasis = TaffyDimension.content()`
  lines (the bug fix below).

`checkGenerated` is taffy-only and only checks categories that already have
committed output. **WPT output is not committed and is deliberately not part of
`:gentest:build` or the default check gate**; use `checkWpt`/`checkAll`
explicitly once such output is checked in.

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
* **WPT direct tests are not currently committed** under `src/test`; the WPT
  adapter is verified with a self-contained sample and a sparse WPT checkout.
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
