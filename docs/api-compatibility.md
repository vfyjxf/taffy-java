# API Compatibility Checking

The `check` lifecycle verifies the current API against the last published
release of `dev.vfyjxf:taffy` using
[JarCompatibilityChecker](https://github.com/neoforged/JarCompatibilityChecker).
The check is implemented as a small Gradle plugin in `buildSrc`
(`dev.vfyjxf.taffy.api-compatibility`), so an accidental API break fails the
build early.

## Baseline

The baseline is the **published plain jar** resolved from Maven Central:

| Property | Value |
| --- | --- |
| Coordinates | `dev.vfyjxf:taffy` |
| Default version | `1.1.5` |
| Resolution | `mavenCentral()`, `transitive = false` |
| Override | `-PapiCompatibilityBaselineVersion=<version>` |

The baseline version is deliberately independent of `project.version` in
`build.gradle`. The project version stays at `1.1.4` while the compatibility
baseline is the already-published `1.1.5`; bump the baseline only after a new
release has been published.

### Why the plain jar and not `shadowJar`

The artifact uploaded to Maven Central is built from the `jar` task
(`from components.java`), not from `shadowJar`:

- `shadowJar` relocates `it.unimi.dsi.fastutil` to
  `dev.vfyjxf.taffy.shadow.fastutil`, so every fastutil reference in the public
  API would look changed to the checker.
- `shadowJar` is minimized and contains shaded third-party classes that are not
  part of the published artifact, producing false class/field/method
  incompatibilities.

The check therefore compares `build/libs/taffy-java-<version>.jar` (the plain
jar) with `dev.vfyjxf:taffy:1.1.5` from Maven Central.

## Tasks

```bash
# Full check, wired into ./gradlew check and ./gradlew build
./gradlew checkApiCompatibility

# Only run JarCompatibilityChecker and write the JSON report
./gradlew checkApiCompatibilityWithJarCompatibilityChecker

# Use a different baseline release
./gradlew checkApiCompatibility -PapiCompatibilityBaselineVersion=1.1.5
```

- `checkApiCompatibilityWithJarCompatibilityChecker` is a `JavaExec` task.
  It runs `net.neoforged.jarcompatibilitychecker.ConsoleTool` on the
  `apiCompatibilityChecker` classpath and writes
  `build/reports/apiCompatibility/report.json`.
- `checkApiCompatibility` reads that report and fails the build if any
  incompatibility has `"isError": true`.
- `check` (and therefore `build`) depends on `checkApiCompatibility`.

### Checker dependencies

| Dependency | Version | Notes |
| --- | --- | --- |
| `net.neoforged:jarcompatibilitychecker` | `0.1.23` | from `https://maven.neoforged.net/releases` |
| `org.ow2.asm:asm`, `asm-analysis`, `asm-commons`, `asm-tree` | `9.10.1` | forced via `resolutionStrategy.eachDependency` |

The NeoForged repository is content-filtered to the `net.neoforged` group, so
only the checker (and not its Guava/Gson/jopt-simple dependencies) is fetched
from it. The ASM version is pinned because older ASM releases cannot read newer
class files and to keep results identical to JEI's setup. All dependencies are
ordinary Gradle module dependencies, so a warm Gradle cache makes
`./gradlew checkApiCompatibility --offline` work.

## Reading a failure

When the check fails, Gradle prints one line per error, in the form:

```
API compatibility check failed against dev.vfyjxf:taffy:1.1.5 from https://repo.maven.apache.org/maven2:
  dev/vfyjxf/taffy/style/TaffyStyle: itemIsTable:Z - Field was lowered in visibility
```

The report itself is a JSON object keyed by the internal class name:

```json
{
  "dev/vfyjxf/taffy/style/TaffyStyle": {
    "classIncompatibilities": [],
    "methodIncompatibilities": {},
    "fieldIncompatibilities": {
      "itemIsTable:Z": [
        {"message": "Field was lowered in visibility", "isError": true}
      ]
    }
  }
}
```

- `classIncompatibilities` covers the class itself (superclass/interface
  changes, visibility, abstract/final changes, ...).
- `methodIncompatibilities` is keyed by method name and descriptor.
- `fieldIncompatibilities` is keyed by field name and descriptor.
- Only entries with `"isError": true` fail the build; warnings are reported by
  the checker but do not fail it.

To fix a failure, either restore the removed/changed public API member or make
the change in a new major release and update the baseline deliberately. Do not
add blanket ignores: the validator mirrors JEI's logic and only inspects the
`isError` flag.

## What `--api` means

The checker supports two compatibility modes:

- `--api` — checks compatibility of the **public and protected API** of the
  base jar (the published release). This is the mode used here, matching JEI.
- `--binary` (default when `--api` is absent) — checks **all** members,
  including private ones, which is stricter than what downstream consumers can
  observe.

`ConsoleTool` exits with status 0 unless `--fail` is passed, so the Gradle
validator task, not the process exit code, is what fails the build.

## Upgrading the baseline

1. Publish a new release to Maven Central.
2. Run the check against it:
   ```bash
   ./gradlew checkApiCompatibility -PapiCompatibilityBaselineVersion=<new-version>
   ```
3. If it passes, update the default in
   `buildSrc/src/main/java/dev/vfyjxf/taffy/gradle/ApiCompatibilityPlugin.java`
   (`DEFAULT_BASELINE_VERSION`). The default lives in the plugin so a plain
   `./gradlew check` always compares against the intended release.
4. Investigate every reported error before bumping: a baseline bump is a promise
   that the new API is compatible with the previous release.

## Reproducibility notes

- The report path is fixed: `build/reports/apiCompatibility/report.json`.
- The checker task is skipped when its inputs (checker classpath, baseline jar,
  input jar, arguments) have not changed; the validator task is cheap and always
  re-reads the report.
- The baseline jar is resolved through Gradle's normal dependency cache. It is
  never downloaded by ad-hoc HTTP code, so offline builds and CI caches behave
  like any other dependency.
