## 1.0.58
# Release Notes - teswiz v1.0.58

_Previous release: v1.0.57_

This release makes teswiz's own outgoing calls robust to a host whose addresses are only partly reachable: each
address is tried once, before anything is sent, and every skipped address is recorded as a network note (log,
scenario, end-of-run file). App downloads from a URL now use the same fallback. A failed Slack or email notification
is diagnosable from the log alone, the Slack POST goes through the proxy teswiz is already configured with, and both
delivery timeouts are configurable. Playwright API engine failures now log the host's resolved addresses.

## ⬆ Upgrade Notes

- **No public API is removed or changed.** New public overloads: `SlackSender(Duration timeout)`,
  `EmailSender(SmtpSettings, Duration timeout)` and `EmailSender.sessionProperties(SmtpSettings, Duration)`. New
  public package `com.znsio.teswiz.network` (`FallbackHttpClient`, `HttpRequest`, `HttpResponse`, `AddressFallback`,
  `AddressFallbackSettings`, `NetworkNote`, `NetworkNotes`, `RunNetworkNotes`, `ProxySettings`, `FailureDetail`,
  `AddressResolver`), new `listener.ScenarioNetworkNotes` and `TEST_CONTEXT.NETWORK_NOTES`.
- **New config keys (template order, after `PROXY_URL`):** `HTTP_ADDRESS_FALLBACK` (default `true`) and
  `HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS` (default `5`). Add both (commented is fine) to every execution
  `.properties` file, or `validateConfigurationTemplates` fails.
- **Behaviour change — address fallback:** the Slack POST, `HeartBeat` and URL-based app downloads
  (`AppPathResolver`) now go through `FallbackHttpClient` (OkHttp). When a host has several addresses and one cannot
  be connected to within 5s, the next one is tried. Set `HTTP_ADDRESS_FALLBACK=false` to try only the first address.
- **Behaviour change — reports:** every run writes `network-notes.json` (`[]` when empty) to the reports directory.
- **Behaviour change — HeartBeat:** each check now times out after 30s (before: no timeout).
- **Behaviour change — Slack timeout:** the default connect and request timeout of the Slack POST rises from 10s to
  20s. Set `notify.slack.timeout.seconds` to change it. The SMTP default stays 15s (`notify.smtp.timeout.seconds`).
- **Behaviour change — proxy:** the Slack POST now goes through the proxy teswiz resolves from `PROXY_KEY` /
  `PROXY_URL` (the same contract `Setup` uses for browsers and drivers). With no proxy configured nothing changes.
- **Behaviour change — app download timeout:** `APP_DOWNLOAD_TIMEOUT_SECONDS` still limits the connect and each read.
  With address fallback on, each address gets `HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS` to connect.
- **Behaviour change — download failure message:** a transfer that fails part-way now reports
  `Unable to download file '<url>'` (with the cause); a non-200 answer still reports
  `Unable to connect to url: '<url>'. Got connection error '<status>'`.

## ✨ Improvements

### Address fallback and network notes

A host such as `hooks.slack.com` resolves to many rotating addresses. When an egress firewall blocks only some of
them, a client that tries just the first address fails intermittently. teswiz's shared `FallbackHttpClient` tries each
address once (5s connect timeout each), falls back only before anything is sent (so a POST is never sent twice), and
records a network note for every skipped address. Notes never change a result.

```
WARN NetworkNotes - Network note: hooks.slack.com:443 (slack): 54.0.0.1 unreachable
     (SocketTimeoutException: connect timed out) -> connected via 54.0.0.2
WARN RunNetworkNotes - Network notes for this run: 1 host(s) needed address fallback
  hooks.slack.com:443  fallbacks=1  unreachable=[54.0.0.1]  used=[54.0.0.2]
```

Notes also go to the running scenario (ReportPortal log and `TEST_CONTEXT.NETWORK_NOTES`) and to
`network-notes.json` in the reports directory. OkHttp (`Api`), Playwright and REST Assured already try every address
natively and record no notes; SMTP (`jakarta.mail`) and telemetry are not covered. See
`docs/subsystems/network-address-fallback.md`.

### App download uses address fallback ([#82](https://github.com/anandbagmar/teswiz/issues/82))

When `APP_PATH` is a URL, `AppPathResolver` now validates it (HEAD) and downloads it (GET) through
`FallbackHttpClient`, so a multi-address host (S3, Artifactory, a CDN) with some blocked addresses no longer fails the
run before any test starts. Skipped addresses are recorded as network notes with purpose `app-download`. The new
`FallbackHttpClient.download(HttpRequest, Path)` streams the body to a file (never held in memory) and deletes a
partial file when the transfer fails; `HttpRequest.head(...)` sends a HEAD. Local file paths and cloud references
(`bs://`, `lt://`) are unchanged.

### Failed notifications say why

A failed Slack or email delivery now logs the exception type and root cause, the destination host (and SMTP port),
the timeout and the proxy used. The Slack webhook URL and the SMTP password are never logged; an exception that echoes
one is redacted. Before, a failure logged only the exception message, e.g. `HTTP connect timed out`, which does not
say what could not be reached.

```
WARN SlackSender - Slack delivery failed; swallowed so the run is unaffected:
     SocketTimeoutException: connect timed out [host=hooks.slack.com, timeout=20s, proxy=none]
```

See "Diagnosing a failed delivery" in `docs/subsystems/run-notifications.md`.

### Configurable delivery timeouts

`notify.slack.timeout.seconds` (default 20) and `notify.smtp.timeout.seconds` (default 15) in
`notifications.properties`, overridable by `NOTIFY_SLACK_TIMEOUT_SECONDS` / `NOTIFY_SMTP_TIMEOUT_SECONDS`. A blank,
non-numeric or non-positive value uses the default.

### Playwright API failures show the host's addresses

When a Playwright API engine request fails after all retries, the error log now also lists the IP addresses the
request's host resolves to:

```
ERROR PlaywrightApiEngineClient - API request GET https://api.example.com/v1 failed after 3 attempts
      | Host addresses: api.example.com -> [203.0.113.10, 203.0.113.11] | Cause: Timeout 60000ms exceeded.
```

An unresolvable host is reported as `unresolved (...)`. This helps tell an unreachable address from a slow server.
Each attempt still waits the full `TESWIZ_API_TIMEOUT_SECONDS` (default 60), so a call that never answers takes about
`API_MAX_RETRIES` × that value before it fails.

## 🛠 CI

- **API Engines CI:** sets `TESWIZ_API_TIMEOUT_SECONDS=15`, so a stalled third-party call fails in about 50s instead
  of about 3 minutes across 3 attempts. Local runs keep the 60s default.

## 📚 Documentation

- New `docs/subsystems/network-address-fallback.md`: how address fallback works, its settings, which connections it
  covers, and how to use `FallbackHttpClient` from your own code.
- `docs/configuration/configuration-parameters.md`: `TESWIZ_API_TIMEOUT_SECONDS` is documented as a per-attempt
  timeout, multiplied by `API_MAX_RETRIES`.
- `docs/engines/running-api-tests.md`: describes the Playwright request timeout and host address logging.
- `docs/subsystems/run-notifications.md`: "Diagnosing a failed delivery".

## 1.0.58
# Release Notes - teswiz v1.0.58 (unreleased — 1.0.58-SNAPSHOT)

_Previous release: v1.0.57_

This release makes teswiz's own outgoing calls robust to a host whose addresses are only partly reachable: each
address is tried once, before anything is sent, and every skipped address is recorded as a network note (log,
scenario, end-of-run file). It also makes a failed Slack or email notification diagnosable from the log alone, sends
the Slack POST through the proxy teswiz is already configured with, and makes both delivery timeouts configurable.

## ⬆️ Upgrade Notes

- **No public API is removed or changed.** New public overloads: `SlackSender(Duration timeout)`,
  `EmailSender(SmtpSettings, Duration timeout)` and `EmailSender.sessionProperties(SmtpSettings, Duration)`. New
  public package `com.znsio.teswiz.network` (`FallbackHttpClient`, `HttpRequest`, `HttpResponse`, `AddressFallback`,
  `AddressFallbackSettings`, `NetworkNote`, `NetworkNotes`, `RunNetworkNotes`, `ProxySettings`, `FailureDetail`,
  `AddressResolver`), new `listener.ScenarioNetworkNotes` and `TEST_CONTEXT.NETWORK_NOTES`.
- **New config keys (template order, after `PROXY_URL`):** `HTTP_ADDRESS_FALLBACK` (default `true`) and
  `HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS` (default `5`). Add both (commented is fine) to every execution
  `.properties` file, or `validateConfigurationTemplates` fails.
- **Behaviour change — address fallback:** the Slack POST and `HeartBeat` now go through `FallbackHttpClient` (OkHttp).
  When a host has several addresses and one cannot be connected to within 5s, the next one is tried. Set
  `HTTP_ADDRESS_FALLBACK=false` to try only the first address.
- **Behaviour change — reports:** every run writes `network-notes.json` (`[]` when empty) to the reports directory.
- **Behaviour change — HeartBeat:** each check now times out after 30s (before: no timeout).
- **Behaviour change — Slack timeout:** the default connect and request timeout of the Slack POST rises from 10s to
  20s. Set `notify.slack.timeout.seconds` to change it. The SMTP default stays 15s (`notify.smtp.timeout.seconds`).
- **Behaviour change — proxy:** the Slack POST now goes through the proxy teswiz resolves from `PROXY_KEY` /
  `PROXY_URL` (the same contract `Setup` uses for browsers and drivers). With no proxy configured nothing changes.

## ✨ Improvements

### Failed notifications say why

A failed Slack or email delivery now logs the exception type and root cause, the destination host (and SMTP port),
the timeout and the proxy used. The Slack webhook URL and the SMTP password are never logged; an exception that echoes
one is redacted. Before, a failure logged only the exception message, e.g. `HTTP connect timed out`, which does not
say what could not be reached.

```
WARN SlackSender - Slack delivery failed; swallowed so the run is unaffected:
     SocketTimeoutException: connect timed out [host=hooks.slack.com, timeout=20s, proxy=none]
```

See "Diagnosing a failed delivery" in `docs/subsystems/run-notifications.md`.

### Address fallback and network notes

A host such as `hooks.slack.com` resolves to many rotating addresses. When an egress firewall blocks only some of
them, a client that tries just the first address fails intermittently. teswiz's shared `FallbackHttpClient` tries each
address once (5s connect timeout each), falls back only before anything is sent (so a POST is never sent twice), and
records a network note for every skipped address. Notes never change a result.

```
WARN NetworkNotes - Network note: hooks.slack.com:443 (slack): 54.0.0.1 unreachable
     (SocketTimeoutException: connect timed out) -> connected via 54.0.0.2
WARN RunNetworkNotes - Network notes for this run: 1 host(s) needed address fallback
  hooks.slack.com:443  fallbacks=1  unreachable=[54.0.0.1]  used=[54.0.0.2]
```

Notes also go to the running scenario (ReportPortal log and `TEST_CONTEXT.NETWORK_NOTES`) and to
`network-notes.json` in the reports directory. OkHttp (`Api`), Playwright and REST Assured already try every address
natively and record no notes; SMTP (`jakarta.mail`) and telemetry are not covered. See
`docs/subsystems/network-address-fallback.md`.

### App download uses address fallback ([#82](https://github.com/anandbagmar/teswiz/issues/82))

When `APP_PATH` is a URL, `AppPathResolver` now validates it (HEAD) and downloads it (GET) through
`FallbackHttpClient`, so a multi-address host (S3, Artifactory, a CDN) with some blocked addresses no longer fails the
run before any test starts. Skipped addresses are recorded as network notes with purpose `app-download`. The new
`FallbackHttpClient.download(HttpRequest, Path)` streams the body to a file (never held in memory) and deletes a
partial file when the transfer fails; `HttpRequest.head(...)` sends a HEAD. Local file paths and cloud references
(`bs://`, `lt://`) are unchanged.

- **Behaviour change — app download timeout:** `APP_DOWNLOAD_TIMEOUT_SECONDS` still limits the connect and each read
  (as before). With address fallback on, each address gets `HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS` to connect.
- **Behaviour change — download failure message:** a transfer that fails part-way now reports
  `Unable to download file '<url>'` (with the cause); a non-200 answer still reports
  `Unable to connect to url: '<url>'. Got connection error '<status>'`.

### Configurable delivery timeouts

`notify.slack.timeout.seconds` (default 20) and `notify.smtp.timeout.seconds` (default 15) in
`notifications.properties`, overridable by `NOTIFY_SLACK_TIMEOUT_SECONDS` / `NOTIFY_SMTP_TIMEOUT_SECONDS`. A blank,
non-numeric or non-positive value uses the default.

### Playwright API failures show the host's addresses

When a Playwright API engine request fails after all retries, the error log now also lists the IP addresses the
request's host resolves to (`Host addresses: <host> -> [<ip>, ...]`, or `unresolved (...)`). A request that timed out
on every attempt can then be matched against blocked or unreachable addresses. Each attempt still waits the full
`TESWIZ_API_TIMEOUT_SECONDS`; the API Engines CI workflow sets it to 15 so a stalled third-party call fails in about
50s instead of about 3 minutes.

## 1.0.57
# Release Notes - teswiz v1.0.57

_Previous release: v1.0.56_

This release moves the build to Gradle 9.9.0, fixes the fat jar's `Main-Class` and `Runner.main` argument handling, and removes every Gradle 10 deprecation warning. It adds a report-only PMD complexity report and ArchUnit dependency rules, and lowers complexity in the Playwright, OCR, image-matching and command-line code with no public API changes. Library code can now read configuration through the new `TeswizConfigReader`. CI moves off the deprecated `gradle/gradle-build-action`, every GitHub Action is on its latest major version, Linux runners are pinned to `ubuntu-24.04`, and the unit-test workflow no longer prints token fragments to the run log.

## ⬆️ Upgrade Notes

- **No public API is removed or changed.** A `javap` comparison of all compiled main classes against 1.0.56 shows no removed or changed public or protected members. The new public types are `com.znsio.teswiz.config.TeswizConfigReader`, `com.znsio.teswiz.config.TeswizConfig`, `com.znsio.teswiz.visual.ApplitoolsConfigValues` and `com.znsio.teswiz.visual.PositionIndexParser`.
- **Newly deprecated:** the public field `CucumberScenarioReporterListener.appiumServerManager`. It still works. Nothing in teswiz reads it any more.
- **Node.js minimum:** Node.js 20.19+, 22.12+ or 24+ with npm 10+ (22.12+ or 24+ for `WEB_ENGINE=playwright-ts`). Earlier docs said Node.js 18+, which Appium 3 does not support.
- **npm `playwright` in consumer projects:** keep it on the same minor as teswiz's Playwright Java (1.64). Install it with `npm install --save-exact playwright@1.64`. A different minor downloads a different Chromium build, and Playwright Java fails with "Executable doesn't exist".

## 🔒 Security

### Token fragments no longer printed in CI logs

`Build_And_Run_Unit_Tests_CI.yml` had two debug steps that echoed the first and last 15 characters (and the first 4) of `TESWIZ_REPO_DISPATCH_TOKEN` and `TESWIZ_PAT`. GitHub masks only the full secret value, so these fragments appeared in plain text in every run log. Both steps are removed, and both tokens have been rotated. The secrets are still used, unchanged, by the README update and repository-dispatch steps.

## 🐛 Fixes

### `java -jar` on the fat jar finds the main class

The `shadowJar` manifest declared `Main-Class: com.znsio.e2e.runner.Runner`, a class that does not exist, so `java -jar teswiz-<version>-all.jar` failed with `ClassNotFoundException`. It now points to `com.znsio.teswiz.runner.Runner`, the same class the Gradle `run` task uses.

### `Runner.main` reports bad arguments instead of a `NullPointerException`

`Runner.main` copied the `LOG_DIR` system property into `OUTPUT_DIRECTORY` before checking its arguments. Without `LOG_DIR` (for example `java -jar teswiz-<version>-all.jar` with no arguments) that threw a `NullPointerException`, and with bad arguments it overwrote `OUTPUT_DIRECTORY` before failing. It now checks the three arguments first and fails with the usage message (`configFilePath`, `stepDefDirName`, `featuresDirName`). It sets `OUTPUT_DIRECTORY` only when `LOG_DIR` is provided, and otherwise the existing `target` default applies.

### Sample capability files resolve on Linux

Four sample capability files did not match the case of the name that loads them. They were found on macOS and Windows, where file names are case-insensitive, but would fail on Linux. They are renamed to match:

- `caps/dineout_browserstack_capabilities.json` and `caps/jio/jiomeet_browserstack_capabilities.json`, the names their configs' `CAPS` use.
- `caps/jio/jiomeetLatest_capabilities.json` and `caps/jio/jiomeetOldVersion_capabilities.json`, which the multi-app JioMeet scenarios load as `<appName>_capabilities.json`.

Two files that nothing referenced are removed: `caps/theapp/myjio_headspin_ios_capabilities.json` and `caps/theapp/theapp_local_capabilities.json`.

### README release badge updates again

The unit-test workflow updates the README release badge with the regex `release-[^-]+-blue\.svg\)`, but the badge URL ended in `-blue.svg?style=flat)`, so the regex never matched and the badge stayed at 1.0.40. The badge now ends in `-blue.svg)` (flat is the shields.io default) and shows the current release.

### Sample step definitions log the message, not a stream

21 log calls across 11 sample step files used `LOGGER.info(System.out.printf(...))`. `printf` prints to stdout and returns the `PrintStream`, so the log line held a stream reference instead of the message. They now use `String.format(...)`.

## 🧮 Code Quality

### PMD complexity report (report-only)

The build now runs PMD 7.29.0 with `config/pmd/teswiz-complexity-ruleset.xml`. Its thresholds mirror the code-quality rules in `AGENTS.md`:

- cyclomatic complexity: 10 per method
- cognitive complexity: 15
- NPath complexity: 200
- statement count: 30 per method
- `if` nesting: 3
- god classes

It is report-only (`ignoreFailures = true`). It runs with `check` and `build` on main sources, writes `build/reports/pmd/main.html`, and never fails the build. The unit-test workflow uploads the report with the test results. Run `./gradlew pmdMain` for a report on its own.

The starting baseline was 92 findings in 40 files. After the refactors below it is 65 findings in 36 files.

### Behaviour-preserving complexity refactors

Each refactor started with characterization tests that passed against the original code, and kept them green through the change. No public or protected member changed.

- **Playwright context options.** `PlaywrightJavaDriverManager.buildContextOptions` (cyclomatic complexity 27, the highest in the codebase) delegates to the new package-private `PlaywrightContextOptionsMapper`, which maps each context option in its own helper. Pinned by `PlaywrightContextOptionsMapperTest` (12 cases).
- **Playwright browser configuration.** `PlaywrightBrowserConfigResolver.resolve` (complexity 24, 88 lines) is split into helpers for headless mode, launch arguments, maximized windows, context options, the default viewport, HTTPS errors, launch options, proxy and channel. Covered by the existing `PlaywrightBrowserConfigResolverTest`.
- **One position-index parser.** `VisualOcrBL.parsePositionIndex` and `AbstractVisualOcrScreen.parsePositionIndex` had identical copies of the "first / last / 3rd / 2" parsing. Both keep their signatures and delegate to the new `com.znsio.teswiz.visual.PositionIndexParser`. Pinned by `PositionIndexParserTest` (28 cases).
- **`CommandLineExecutor`.** `execCommandInternal` (complexity 16, 89 lines) is split into process creation, exit-status and timeout handling, logging, the output artifact and stream closing. Pinned by `CommandLineExecutorTest` (9 cases with real processes).
- **`PlaywrightJavaWebDriver`.** The anonymous Selenium `TargetLocator`, `Navigation`, `Options`, `Timeouts`, `Window` and `Logs` implementations are now named private classes, and cookie conversion lives in the package-private `PlaywrightJavaCookieMapper`. Pinned by `PlaywrightJavaWebDriverSeleniumApiTest` (34 cases).
- **`OcrService`.** `findTextMatch`, `findAllTextMatches` and `readText` share screenshot preparation and Tesseract set-up instead of repeating them. Each method keeps its own error handling. Pinned by `OcrServiceBehaviourTest` (13 cases using real Tesseract).
- **`ImageRecognitionService`.** The two template finders share scene preparation and template loading, and the multi-scale matching loops use small helpers. Every OpenCV matrix is still released in a `finally`. Pinned by `ImageRecognitionServiceBehaviourTest` (9 cases using real OpenCV).

The OCR and image-matching tests skip themselves on a machine without the native Tesseract or OpenCV libraries, instead of failing.

### CodeQL quality queries

`codeql.yml` now runs the `security-and-quality` query suite, which adds maintainability and reliability queries to the default security queries. Findings appear under Security → Code scanning.

## 🏗 Design Improvements (non-breaking)

These changes loosen the coupling between the library packages and `com.znsio.teswiz.runner` without breaking any public API.

- **Architecture rules (ArchUnit).** The new `RunnerDependencyRulesTest` freezes the existing dependencies of library packages on `runner`. Library packages are everything except `runner`, `steps`, `testng`, `screen`, `listener` and `businessLayer`. Existing dependencies are recorded in `src/test/resources/archunit_store`. A new one fails the build, and removed ones drop out of the store. A second, strict rule keeps `mobile.session` independent of `runner`.
- **Read-only configuration interface.** The new `TeswizConfigReader` interface and `TeswizConfig.reader()` give library code read access to the execution configuration without depending on `runner.Setup`. `reader()` returns a live view, so a reader obtained before `Setup` is initialised sees the values later. All 91 library calls to `Setup`'s config getters now go through it, and frozen `runner` dependencies drop from 440 to 349. `Setup`'s getters are unchanged for existing callers.
- **Off the deprecated Appium server shim.** `AppiumDriverManager` and `PluginCliRequest` use `AppiumServerController` directly instead of the deprecated `AppiumServerManager`. The public field `CucumberScenarioReporterListener.appiumServerManager` is still initialised, and is now marked `@Deprecated`.
- **Platform dispatch in `Drivers`.** The `switch` statements over `Platform` that created and closed drivers are replaced by a per-platform registry. Driver managers still load lazily, and a `null` platform still throws a `NullPointerException`. Pinned by `DriversPlatformDispatchTest` (13 cases).
- **Applitools configuration values.** The new `ApplitoolsConfigValues` reads the server URL, default match level, viewport size and the benchmarking and browser-fetching flags from the loaded Applitools configuration, as pure functions. `Setup` delegates to it with unchanged fallbacks: an unknown match level becomes `STRICT`, a missing viewport uses the runtime defaults, and a malformed viewport still fails. Pinned by `ApplitoolsConfigValuesTest` (10 cases).

For contributors: in library packages, read configuration through `TeswizConfig.reader()` rather than `Setup`'s static getters. If `RunnerDependencyRulesTest` fails, remove the new `runner` dependency instead of adding it to the store.

## 🧹 Build and Repository Cleanup

- **Reproducible versions.** The dynamic `org.assertj:assertj-core:3+` and `org.apache.logging.log4j:log4j-core:2+` are pinned to the versions they resolved to, 3.27.7 and 2.26.1.
- **No Gradle 10 deprecation warnings.** `downloadDependencies` reads the `forceUpdate` project property at configuration time instead of calling `Task.project` inside `doLast`. The sources jar is built by `assemble` instead of being declared in the deprecated `archives` configuration, and the publication still includes it. A build with `--warning-mode=all` reports no deprecations.
- **One dependency bot.** `renovate.json` is removed. Dependabot is the only update bot.
- **One CodeQL workflow.** `codeql-analysis.yml` duplicated `codeql.yml` on every push and pull request, so it is removed. `codeql.yml` scans Java and the GitHub Actions workflows, and the README badge points to it.
- **`build.gradle.sample`** wrapper `gradleVersion` moves from 9.7.1 to 9.9.0, matching the build.
- **`package.json`** gets a plain-text description instead of four `znsio/teswiz` badge links, and drops `"main": "index.js"`, which pointed to a file that does not exist.
- **Changelog.** Duplicate releases and unreleased SNAPSHOT drafts are removed, with their unique notes folded into the matching release. The 1.0.42 section is retitled (it said v1.0.41) and lists only its own changes. Unclosed code fences that turned the rest of the file into one code block are closed.

## 🛠 CI / GitHub Actions

### Linux runners pinned to `ubuntu-24.04`

GitHub is moving the `ubuntu-latest` label to Ubuntu 26.04 between October 19 and November 19, 2026 ([actions/runner-images#14748](https://github.com/actions/runner-images/issues/14748)). All Linux jobs, including the CodeQL matrix, now run on `ubuntu-24.04` explicitly, so the image cannot change underneath the build. The Playwright `--with-deps` install and the Tesseract native libraries behind the `tess4j` 5.11.0 pin are the parts most likely to differ on 26.04. Move to `ubuntu-26.04` one workflow at a time, once it is verified.

### `gradle/gradle-build-action` replaced by `gradle/actions/setup-gradle`

The unit-test workflow used `gradle/gradle-build-action@v3` and ran Gradle through its `arguments` input, and both are deprecated. It now runs `gradle/actions/setup-gradle@v6`, then calls the wrapper directly with `./gradlew clean build --refresh-dependencies`.

### Actions on their latest major versions

| Action | From | To |
|---|---|---|
| `actions/setup-node` | v5 | v7 |
| `actions/setup-java` | v5 | v6 |
| `actions/github-script` (`cleanup-branch-artifacts`) | v8 | v9 |
| `gradle/gradle-build-action` → `gradle/actions/setup-gradle` | v3 | v6 |

`actions/checkout@v7`, `actions/upload-artifact@v7`, `github/codeql-action@v4` and `peter-evans/repository-dispatch@v4` were already current. None of the breaking changes in these majors affect the teswiz workflows.

### Dependabot

- **GitHub Actions tracked.** A `github-actions` ecosystem now covers `.github/workflows` and the composite actions under `.github/actions/*`.
- **npm `playwright` held to the Java minor.** Minor and major updates of npm `playwright` are ignored, because it must stay on the same minor as `com.microsoft.playwright:playwright` (see Upgrade Notes). Bump both together once the Java release for the new minor ships.

## 📦 Dependency Updates

| Dependency | From | To |
|---|---|---|
| Gradle wrapper | 9.8.2 | 9.9.0 |
| `com.fasterxml.jackson.core:jackson-databind` | 2.22.4 | 2.23.0 |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` | 2.22.4 | 2.23.0 |
| `com.fasterxml.jackson.datatype:jackson-datatype-jdk8` | 2.22.4 | 2.23.0 |
| `com.applitools:eyes-images-java5` | 5.83.18 | 5.83.20 |
| `net.masterthought:cucumber-reporting` | 5.12.0 | 5.12.1 |
| `com.google.auth:google-auth-library-oauth2-http` (optional, `compileOnly` / test) | 1.55.0 | 1.57.0 |
| `com.tngtech.archunit:archunit-junit5` (test only) | — | 1.5.1 (new) |
| PMD (Gradle `pmd` plugin `toolVersion`, build only) | — | 7.29.0 (new) |
| npm `@applitools/eyes-playwright` | 1.51.7 | 1.52.0 |

Notes:

- **Gradle 9.9.0.** The `wrapper { gradleVersion }` block in `build.gradle` moves with the wrapper files, so `./gradlew wrapper` no longer reverts the upgrade. None of the 9.9.0 breaking changes apply. If the first `./gradlew` run hangs with no output, delete `~/.gradle/wrapper/dists/gradle-9.9.0-bin` and run again.
- **Jackson 2.23.0** still targets Java 8 bytecode, so it is compatible with the Java 17 baseline.
- **`npm audit`** is unchanged: 0 critical, 0 high, 36 moderate, 1 low.

### Not upgraded

- **npm `playwright` 1.65.0:** held at 1.64.1 to match Playwright Java 1.64.0, the latest Java release.
- **`net.sourceforge.tess4j:tess4j` 5.21.0:** stays pinned at 5.11.0 to avoid the `libtesseract` missing-symbol error (`pixBackgroundNormTo1MinMax`) on Linux runners.
- **`log4j-core` 3.0.0-beta3, `assertj-core` 4.0.0-M1:** pre-releases, not taken.
- **Appium driver majors:** `appium-uiautomator2-driver` 8.7.3 and `appium-xcuitest-driver` 12.22.8 are the latest releases that support Appium 3. The 9.x and 13.x lines need Appium 4. Appium 3.9.0 is the latest stable.
- Gradle plugins (`org.sonarqube`, `com.gradleup.shadow`, `io.freefair.aspectj.post-compile-weaving`, `io.github.ben-manes.versions`) were already on their latest releases.

## ✅ Tests

- New: `RunnerMainArgumentsTest`, `PlaywrightContextOptionsMapperTest`, `PositionIndexParserTest`, `CommandLineExecutorTest`, `PlaywrightJavaWebDriverSeleniumApiTest`, `OcrServiceBehaviourTest`, `ImageRecognitionServiceBehaviourTest`, `RunnerDependencyRulesTest`, `DriversPlatformDispatchTest` and `ApplitoolsConfigValuesTest`.
- No existing test was changed.
- `./gradlew build` passes: 1,090 tests, 0 failures, 15 skipped (the existing opt-in tests). This includes `validateConfigurationTemplates`.
- Every CI workflow passes on `main`, including the BrowserStack, Visual OCR, API engine, HardGate, TestNG multi-user and CodeQL workflows.

## 📝 Docs

- **Node.js minimum corrected** in `docs/getting-started/prerequisites.md` and `getting-started-guide.md`: Node.js 20.19+, 22.12+ or 24+ with npm 10+ (22.12+ or 24+ for `WEB_ENGINE=playwright-ts`).
- **Appium 3, not Appium 2,** in `prerequisites.md`, `getting-started-guide.md`, `docs/engines/mobile-appium-setup.md`, `docs/index.md` and `README.md`. The Appium documentation links point to `appium.io/docs/en/latest/`.
- **Pin npm `playwright` in consumer projects.** `docs/architecture-and-internals/playwright-migration-guide.md` (section 6a) installs `playwright@1.64` with `--save-exact` and explains why.
- **Architecture notes.** `docs/architecture-and-internals/architecture-notes.md` adds the `TeswizConfig.reader()` package rule and the ArchUnit dependency rules.
- **Driver creation convention.** `docs/getting-started/writing-first-test.md` now shows the step definition calling `Drivers.createDriverFor(persona, platform, context)` and the business layer only orchestrating screens, as every sample does. The tutorial's old snippet used a `createDriverFor` signature that does not exist.
- **Plans, backlog and history.** `docs/index.md` links the 11 previously unlinked plan, backlog and troubleshooting docs, each plan has an accurate status header, and stale `file:///` and old-path links are fixed.
- **Appium setup.** `prerequisites.md` and `mobile-appium-setup.md` no longer mention the `appium-dashboard` plugin, which is not installed.
- **Agent rules.** `CLAUDE.md`, `ANTIGRAVITY.md` and `.codex/skills/teswiz-project/SKILL.md` point to `AGENTS.md` for the TDD, test-architecture and complexity rules, add the Playwright minor-version rule, and use the current doc paths.

## 🛠 Changed Files Summary

- `build.gradle` (dependency versions, pinned `assertj-core` / `log4j-core`, `Main-Class`, Gradle 10 deprecations, `wrapper` 9.9.0, `pmd` plugin, ArchUnit, version 1.0.57)
- `build.gradle.sample` (`wrapper` 9.9.0)
- `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`
- `package.json`, `package-lock.json`
- `renovate.json` (removed), `.github/workflows/codeql-analysis.yml` (removed)
- `config/pmd/teswiz-complexity-ruleset.xml` (new)
- `src/main/java/com/znsio/teswiz/runner/Runner.java`
- `src/main/java/com/znsio/teswiz/runner/{Setup, Drivers, AppiumDriverManager, PluginCliRequest}.java`
- `src/main/java/com/znsio/teswiz/listener/CucumberScenarioReporterListener.java`
- `src/main/java/com/znsio/teswiz/config/{TeswizConfig, TeswizConfigReader}.java` (new), `src/main/java/com/znsio/teswiz/runner/TeswizConfiguration.java`
- `src/main/java/com/znsio/teswiz/visual/{ApplitoolsConfigValues, PositionIndexParser}.java` (new), `OcrService.java`, `ImageRecognitionService.java`
- `src/main/java/com/znsio/teswiz/web/playwright/{PlaywrightContextOptionsMapper, PlaywrightJavaCookieMapper}.java` (new), `PlaywrightJavaDriverManager.java`, `PlaywrightJavaWebDriver.java`
- `src/main/java/com/znsio/teswiz/config/browser/PlaywrightBrowserConfigResolver.java`
- `src/main/java/com/znsio/teswiz/tools/cmd/CommandLineExecutor.java`
- `src/main/java/com/znsio/teswiz/businessLayer/visualocr/VisualOcrBL.java`, `screen/visualocr/AbstractVisualOcrScreen.java`
- Library packages (91 call sites): `Setup` config reads replaced by `TeswizConfig.reader()`
- `src/test/java/**` (the new tests listed under Tests), `src/test/java/com/znsio/teswiz/steps/*Steps.java` (11 files, `String.format` logging)
- `src/test/resources/archunit.properties`, `src/test/resources/archunit_store/**` (new)
- `caps/dineout_browserstack_capabilities.json`, `caps/jio/jiomeet_browserstack_capabilities.json`, `caps/jio/jiomeetLatest_capabilities.json`, `caps/jio/jiomeetOldVersion_capabilities.json` (renamed); `caps/theapp/myjio_headspin_ios_capabilities.json`, `caps/theapp/theapp_local_capabilities.json` (removed)
- `.github/workflows/*.yml` (`ubuntu-24.04`, setup-node v7, setup-java v6, `security-and-quality`, PMD report artifact; `Build_And_Run_Unit_Tests_CI.yml` setup-gradle and debug steps removed)
- `.github/actions/cleanup-branch-artifacts/action.yml` (github-script v9), `.github/actions/run-and-collect-tests-README.md`
- `.github/dependabot.yml`
- `docs/**`, `README.md`, `CLAUDE.md`, `ANTIGRAVITY.md`, `.codex/skills/teswiz-project/SKILL.md`
- `Changelog.MD`

## 📋 Installation & Upgrade

```groovy
dependencies {
    implementation 'com.github.anandbagmar:teswiz:1.0.57'
}
```

## 1.0.57
# Release Notes - teswiz v1.0.57

_Previous release: v1.0.56_

This release moves the build to Gradle 9.9.0, fixes the fat jar's `Main-Class`, removes every Gradle 10 deprecation warning, picks up the latest Jackson, Applitools, cucumber-reporting and Google Auth versions, corrects the documented Node.js minimum, moves CI off the deprecated `gradle/gradle-build-action`, brings every GitHub Action to its latest major version, and stops the unit-test workflow from printing token fragments to the run log.

## 🔒 Security

### Token fragments no longer printed in CI logs

`Build_And_Run_Unit_Tests_CI.yml` had two debug steps that echoed the first and last 15 characters (and the first 4) of `TESWIZ_REPO_DISPATCH_TOKEN` and `TESWIZ_PAT`. GitHub masks only the full secret value, so these fragments appeared in plain text in every run log. Both steps are removed. The secrets are still used, unchanged, by the README update and repository-dispatch steps.

Treat both tokens as exposed: rotate `TESWIZ_REPO_DISPATCH_TOKEN` and `TESWIZ_PAT`, then update the repository secrets.

## 🐛 Fixes

### `java -jar` on the fat jar finds the main class

The `shadowJar` manifest declared `Main-Class: com.znsio.e2e.runner.Runner`, a class that does not exist, so `java -jar teswiz-<version>-all.jar` failed with `ClassNotFoundException`. It now points to `com.znsio.teswiz.runner.Runner`, the same class the Gradle `run` task uses.

### README release badge updates again

The unit-test workflow updates the README release badge with the regex `release-[^-]+-blue\.svg\)`, but the badge URL ended in `-blue.svg?style=flat)`, so the regex never matched and the badge stayed at 1.0.40. The badge now ends in `-blue.svg)` (flat is the shields.io default) and shows the current release.

### `Runner.main` reports bad arguments instead of a `NullPointerException`

`Runner.main` copied the `LOG_DIR` system property into `OUTPUT_DIRECTORY` before checking its arguments. Without `LOG_DIR` (for example `java -jar teswiz-<version>-all.jar` with no arguments) that threw a `NullPointerException`, and with bad arguments it overwrote `OUTPUT_DIRECTORY` before failing. It now validates the three arguments first and fails with the usage message (`configFilePath`, `stepDefDirName`, `featuresDirName`), and sets `OUTPUT_DIRECTORY` only when `LOG_DIR` is provided (otherwise the existing `target` default applies). Covered by the new `RunnerMainArgumentsTest`.

### Sample step definitions log the message, not a stream

21 log calls across 11 sample step files used `LOGGER.info(System.out.printf(...))`. `printf` prints to stdout and returns the `PrintStream`, so the log line held a stream reference instead of the message. They now use `String.format(...)`.

## 🧮 Code Quality

### PMD complexity report (report-only)

The build now runs PMD 7.29.0 with `config/pmd/teswiz-complexity-ruleset.xml`, whose thresholds mirror the code-quality rules in `AGENTS.md`: cyclomatic complexity (10 per method), cognitive complexity (15), NPath complexity (200), statement count (30 per method), `if` nesting (3), and god classes. It is report-only (`ignoreFailures = true`): it runs with `check` and `build` on main sources, writes `build/reports/pmd/main.html`, and never fails the build. The unit-test workflow uploads the report with the test results. The starting baseline was 92 findings in 40 files; after the refactors below it is 65 findings in 36 files. Run `./gradlew pmdMain` for a report on its own.

### Lower complexity in Playwright browser configuration

- **`PlaywrightJavaDriverManager.buildContextOptions`** (cyclomatic complexity 27, the highest in the codebase) now delegates to the new package-private `PlaywrightContextOptionsMapper`, which maps each context option in its own small helper. Behaviour is unchanged and pinned by the new `PlaywrightContextOptionsMapperTest` (12 cases, including blank strings, partial and `null` viewports, and non-map headers).
- **`PlaywrightBrowserConfigResolver.resolve`** (complexity 24, 88 lines) is split into helpers for headless mode, launch arguments, maximized windows, context options, the default viewport, HTTPS errors, launch options, proxy and channel. Behaviour is unchanged and covered by the existing `PlaywrightBrowserConfigResolverTest`.

### Behaviour-preserving complexity refactors

Each refactor below started with characterization tests that passed against the original code, kept them green through the change, and left the public and protected API of every class unchanged. A `javap` comparison of all compiled main classes shows no removed or changed members and no new public types. PMD findings drop from 92 to 65, and none of these methods is flagged any more.

- **One position-index parser.** `VisualOcrBL.parsePositionIndex` and `AbstractVisualOcrScreen.parsePositionIndex` had identical copies of the "first / last / 3rd / 2" parsing. Both keep their signatures (`public` and `protected final`) and delegate to the new `com.znsio.teswiz.visual.PositionIndexParser`. Pinned by `PositionIndexParserTest` (28 cases).
- **`CommandLineExecutor`.** `execCommandInternal` (complexity 16, 89 lines) is split into process creation, exit-status and timeout handling, logging, output artifact and stream closing. Pinned by the new `CommandLineExecutorTest` (9 cases with real processes: shell pipes, trimmed output, exit codes and stderr, timeout kill, no-shell list form, invalid input, missing tool).
- **`PlaywrightJavaWebDriver`.** The anonymous Selenium `TargetLocator`, `Navigation`, `Options`, `Timeouts`, `Window` and `Logs` implementations are now named private classes, and cookie conversion lives in the package-private `PlaywrightJavaCookieMapper`. `manage()` (cognitive complexity 28) and `switchTo()` (18) are no longer flagged. Pinned by the new `PlaywrightJavaWebDriverSeleniumApiTest` (34 cases: frame switching by index, name, element and parent, windows, alerts, navigation, cookies in both directions, timeouts, window-size fallbacks, logs).
- **`OcrService`.** `findTextMatch`, `findAllTextMatches` and `readText` share screenshot preparation (parse, scale factor, region crop) and Tesseract set-up instead of repeating them; the find fallback chain and the duplicated word-matching loop are their own methods. Each method keeps its own error handling. Pinned by the new `OcrServiceBehaviourTest` (13 cases using real Tesseract on rendered text).
- **`ImageRecognitionService`.** The two template finders share scene preparation and template loading, and the multi-scale matching loops use small helpers; every OpenCV matrix is still released in a `finally`. Pinned by the new `ImageRecognitionServiceBehaviourTest` (9 cases using real OpenCV on a generated scene).

The OCR and image-matching tests skip themselves (instead of failing) on a machine without the native Tesseract or OpenCV libraries; the other cases in those classes still run.

### CodeQL quality queries

`codeql.yml` now runs the `security-and-quality` query suite, which adds maintainability and reliability queries to the default security queries. Findings appear under Security → Code scanning.

## 🏗 Design Improvements (non-breaking)

These changes loosen the coupling between the library packages and `com.znsio.teswiz.runner` without breaking any public API. A `javap` comparison of all compiled main classes shows no removed or changed public or protected members. The only additions are the public types listed below.

- **Architecture rules (ArchUnit).** The new `RunnerDependencyRulesTest` freezes the existing dependencies from library packages (everything except `runner`, `steps`, `testng`, `screen`, `listener` and `businessLayer`) on `runner`. Existing violations are recorded in `src/test/resources/archunit_store`. A new dependency on `runner` fails the build, and fixed ones drop out of the store. A second, strict rule keeps `mobile.session` independent of `runner`.
- **Off the deprecated Appium server shim.** `AppiumDriverManager` and `PluginCliRequest` now use `AppiumServerController` directly instead of the deprecated `AppiumServerManager`. The public field `CucumberScenarioReporterListener.appiumServerManager` is still initialised, and is now marked `@Deprecated`.
- **Read-only configuration interface.** The new `com.znsio.teswiz.config.TeswizConfigReader` interface and `TeswizConfig.reader()` give library code read access to the execution configuration without depending on `runner.Setup`. `reader()` returns a live view, so a reader obtained before `Setup` is initialised sees the values later. `TeswizConfiguration` implements the interface, and all 91 library calls to `Setup`'s config getters now go through it. Frozen `runner` dependencies drop from 440 to 349. `Setup`'s getters are unchanged for callers.
- **Platform dispatch in `Drivers`.** The `switch` statements over `Platform` that created and closed drivers are replaced by a per-platform registry. Driver managers still load lazily, and a `null` platform still throws a `NullPointerException`. Pinned by the new `DriversPlatformDispatchTest` (13 cases).
- **Applitools configuration values.** The new `com.znsio.teswiz.visual.ApplitoolsConfigValues` reads the server URL, default match level, viewport size and the benchmarking and browser-fetching flags from the loaded Applitools configuration. These are pure functions, so they can be tested without loading teswiz. `Setup` delegates to it with unchanged fallbacks: an unknown match level becomes `STRICT`, a missing viewport uses the runtime defaults, and a malformed viewport still fails. Pinned by the new `ApplitoolsConfigValuesTest` (10 cases).

Moving internal-only classes out of `runner` was considered and skipped. The package-private classes (`MobileGestures`, `ElementFinder`, `ElementHighlighter`, `VisualElementWebElementAdapter`, `ConfigFileManager`) would have to become public to move, which widens the API. The public ones (for example `ElementWaiter` and `CucumberArgsBuilder`) are pinned to `runner` by existing tests. Both are better left for a major release.

## 🧹 Build and Repository Cleanup

- **Reproducible versions.** The dynamic `org.assertj:assertj-core:3+` and `org.apache.logging.log4j:log4j-core:2+` are pinned to the versions they resolved to, 3.27.7 and 2.26.1.
- **No Gradle 10 deprecation warnings.** `downloadDependencies` reads the `forceUpdate` project property at configuration time instead of calling `Task.project` inside `doLast`, and the sources jar is built by `assemble` instead of being declared in the deprecated `archives` configuration. The publication still includes it. A build with `--warning-mode=all` now reports no deprecations. The commented-out `com.github.znsio` jitpack entry is removed.
- **One dependency bot.** `renovate.json` is removed. Dependabot (`.github/dependabot.yml`) is the only update bot, and it carries the Appium and Playwright ignore rules.
- **One CodeQL workflow.** `codeql-analysis.yml` duplicated `codeql.yml` ("CodeQL Advanced") on every push and pull request. It is removed; `codeql.yml` scans Java and the GitHub Actions workflows, and the README badge points to it.
- **`build.gradle.sample`** wrapper `gradleVersion` moves from 9.7.1 to 9.9.0, matching the build.
- **`package.json`** gets a plain-text description instead of four `znsio/teswiz` badge links, and drops `"main": "index.js"`, which pointed to a file that does not exist. The lockfile is unchanged.
- **Capability files.** Four sample capability files did not match the case of the name that loads them, so they were found only on case-insensitive file systems (macOS, Windows) and would fail on Linux. They are renamed to match: `caps/dineout_browserstack_capabilities.json` and `caps/jio/jiomeet_browserstack_capabilities.json` (named by their configs' `CAPS`), and `caps/jio/jiomeetLatest_capabilities.json` and `caps/jio/jiomeetOldVersion_capabilities.json` (loaded as `<appName>_capabilities.json` by the multi-app JioMeet scenarios). Two files that nothing referenced are removed: `caps/theapp/myjio_headspin_ios_capabilities.json` and `caps/theapp/theapp_local_capabilities.json`.
- **Changelog.** Removed sections that were fully duplicated: the second 1.0.55 copy and its stray heading, an earlier draft of 1.0.53, `1.0.52-SNAPSHOT`, and repeated 1.0.35 and 1.0.34 copies. The 1.0.44, 1.0.46, 1.0.48 and 1.0.49 SNAPSHOT drafts, an `Unreleased` draft and a stray `1.0.4305` section are gone too. The few notes only they held are now in the matching release (the 1.0.43 self-clearing gestures and their breaking change, two 1.0.44 tests, and 1.0.46 notification details). The 1.0.42 section was titled v1.0.41 and repeated the 1.0.41 notes. It now lists only what changed between the two tags. Unclosed code fences in the 1.0.41 and 1.0.42 install snippets, which turned the rest of the file into a code block, are closed. Old headings now drop the `v` prefix, and a stray mid-file `# Changelog` heading is removed.

## 🛠 CI / GitHub Actions

### Linux runners pinned to `ubuntu-24.04`

GitHub is moving the `ubuntu-latest` label to Ubuntu 26.04 between October 19 and November 19, 2026 ([actions/runner-images#14748](https://github.com/actions/runner-images/issues/14748)), and every workflow showed a migration warning. All Linux jobs, including the CodeQL matrix, now run on `ubuntu-24.04` explicitly, so the image cannot change underneath the build. The Playwright `--with-deps` install and the Tesseract native libraries behind the `tess4j` 5.11.0 pin are the parts most likely to differ on 26.04. Move to `ubuntu-26.04` deliberately, one workflow at a time, once it is verified.

### `gradle/gradle-build-action` replaced by `gradle/actions/setup-gradle`

The unit-test workflow used `gradle/gradle-build-action@v3` and ran Gradle through its `arguments` input. Both are deprecated (the action was replaced by `gradle/actions/setup-gradle`, and `arguments` was removed in v4). The workflow now runs `gradle/actions/setup-gradle@v6`, then calls the wrapper directly with `./gradlew clean build --refresh-dependencies`. This clears both deprecation warnings.

### Actions on their latest major versions

| Action | From | To |
|---|---|---|
| `actions/setup-node` | v5 | v7 |
| `actions/setup-java` | v5 | v6 |
| `actions/github-script` (`cleanup-branch-artifacts`) | v8 | v9 |
| `gradle/gradle-build-action` → `gradle/actions/setup-gradle` | v3 | v6 |

`actions/checkout@v7`, `actions/upload-artifact@v7`, `github/codeql-action@v4` and `peter-evans/repository-dispatch@v4` were already current. None of the breaking changes affect these workflows: they run on GitHub-hosted runners and already set `cache: "npm"` explicitly, and the artifact-cleanup script uses only the injected `github`, `context` and `core`.

### Dependabot

- **GitHub Actions tracked.** A `github-actions` ecosystem now covers `.github/workflows` and the composite actions under `.github/actions/*`, so action versions get update PRs like npm and Gradle dependencies.
- **npm `playwright` held to the Java minor.** Minor and major updates of npm `playwright` are ignored, because it must stay on the same minor as `com.microsoft.playwright:playwright`. Both must use the same browser build: with npm 1.65 (Chromium 1254) next to Java 1.64 (Chromium 1248), each `playwright install` removes the other's Chromium and Playwright-Java tests fail with "Executable doesn't exist". Bump both together once the Java release for the new minor ships, then relax the rule.

## 📦 Dependency Updates

| Dependency | From | To |
|---|---|---|
| Gradle wrapper | 9.8.2 | 9.9.0 |
| `com.fasterxml.jackson.core:jackson-databind` | 2.22.4 | 2.23.0 |
| `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml` | 2.22.4 | 2.23.0 |
| `com.fasterxml.jackson.datatype:jackson-datatype-jdk8` | 2.22.4 | 2.23.0 |
| `com.applitools:eyes-images-java5` | 5.83.18 | 5.83.20 |
| `net.masterthought:cucumber-reporting` | 5.12.0 | 5.12.1 |
| `com.google.auth:google-auth-library-oauth2-http` (optional, `compileOnly` / test) | 1.55.0 | 1.57.0 |
| npm `@applitools/eyes-playwright` | 1.51.7 | 1.52.0 |

Notes:

- **Gradle 9.9.0:** the `wrapper { gradleVersion }` block in `build.gradle` moves with the wrapper files, so `./gradlew wrapper` no longer reverts the upgrade. None of the 9.9.0 breaking changes apply: `build.gradle` throws `GradleException` but does not subclass it. The build still shows two existing deprecation warnings, the same on 9.8.2 and 9.9.0, both failing only in Gradle 10: `Task.project` used at execution time (`project.findProperty('forceUpdate')` inside `downloadDependencies`' `doLast`), and `artifacts { archives sourcesJar }`. If the first `./gradlew` run hangs with no output, delete `~/.gradle/wrapper/dists/gradle-9.9.0-bin` and run again.
- **Jackson 2.23.0** still targets Java 8 bytecode, so it is compatible with the Java 17 baseline.
- **`npm audit`** is unchanged: 0 critical, 0 high, 36 moderate, 1 low.

### Not upgraded

- **npm `playwright` 1.65.0:** held at 1.64.1 to match Playwright Java 1.64.0, the latest Java release (see Dependabot above).
- **`net.sourceforge.tess4j:tess4j` 5.21.0:** stays pinned at 5.11.0 to avoid the `libtesseract` missing-symbol error (`pixBackgroundNormTo1MinMax`) on Linux runners.
- **`log4j-core` 3.0.0-beta3, `assertj-core` 4.0.0-M1:** pre-releases. The dynamic `2+` / `3+` declarations keep resolving the latest stable 2.x / 3.x.
- **Appium driver majors:** `appium-uiautomator2-driver` 8.7.3 and `appium-xcuitest-driver` 12.22.8 are the latest releases that support Appium 3 (9.x / 13.x need Appium 4). Appium 3.9.0 is the latest stable.
- Gradle plugins (`org.sonarqube`, `com.gradleup.shadow`, `io.freefair.aspectj.post-compile-weaving`, `io.github.ben-manes.versions`) were already on their latest releases.

## 📝 Docs

- **Node.js minimum corrected.** `docs/getting-started/prerequisites.md` and `getting-started-guide.md` said Node.js 18+, but Appium 3 and its drivers require `^20.19.0 || ^22.12.0 || >=24.0.0` with npm 10+, and Playwright requires Node.js 20+. The docs now state Node.js 20.19+, 22.12+ or 24+ with npm 10+ (22.12+ or 24+ for `WEB_ENGINE=playwright-ts`).
- **Appium 3, not Appium 2.** `prerequisites.md`, `getting-started-guide.md`, `docs/engines/mobile-appium-setup.md`, `docs/index.md` and `README.md` now say Appium 3, and the Appium documentation links point to `appium.io/docs/en/latest/`. `prerequisites.md` notes that the drivers stay on their Appium 3 lines.
- **Pin npm `playwright` in consumer projects.** `docs/architecture-and-internals/playwright-migration-guide.md` (section 6a) now installs `playwright@1.64` with `--save-exact` instead of an unpinned `playwright`, and explains why npm `playwright` must stay on the same minor as teswiz's Playwright Java (shared browser cache, `Executable doesn't exist` on mismatch).
- **Agent rules.** `CLAUDE.md`, `ANTIGRAVITY.md` and `.codex/skills/teswiz-project/SKILL.md` add a rule to keep npm `playwright` and Playwright Java on the same minor and bump them together. They now point to `AGENTS.md` for the TDD, test-architecture and complexity rules, list it among the files to keep aligned, and use the current doc paths: the old `docs/internals/…`, `docs/features/…` and `docs/guides/…` paths no longer existed. `SKILL.md` names the repository `anandbagmar/teswiz` and separates the framework-shipped steps in `src/main` from the sample steps in `src/test`.
- **Driver creation convention.** `docs/getting-started/writing-first-test.md` said the business layer creates the persona's driver, but every sample does it in the step definition and no BL does. The guide, its layer table, diagram and tutorial now show the step definition calling `Drivers.createDriverFor(persona, platform, context)` and the BL only orchestrating screens. The tutorial's old BL snippet also used a `createDriverFor` signature that does not exist.
- **Plans, backlog and history.** `docs/index.md` links the 11 previously unlinked plan, backlog and troubleshooting docs (including all of `docs/todos/`) in a new section with their status, and `faqs.md` links the jitpack download troubleshooting page. Each plan has an accurate status header. The Playwright API engine plan's 13 `file:///Users/...` links are relative links now, and the TestNG execution-mode plan no longer points to the old `docs/features/`, `docs/internals/` and `docs/guides/` paths.
- **Appium setup.** `prerequisites.md` and `mobile-appium-setup.md` no longer mention the `appium-dashboard` plugin, which is not installed; the manual start command keeps `relaxed-caps` and `device-farm`. Installed components are listed with `npx appium driver list --installed` and `npx appium plugin list --installed`.

## 🛠 Changed Files Summary

- `build.gradle` (dependency versions, pinned `assertj-core`/`log4j-core`, `Main-Class`, Gradle 10 deprecations, `wrapper` 9.9.0)
- `build.gradle.sample` (`wrapper` 9.9.0)
- `renovate.json` (removed), `.github/workflows/codeql-analysis.yml` (removed)
- `src/test/java/com/znsio/teswiz/steps/*Steps.java` (11 files, `String.format` logging)
- `src/main/java/com/znsio/teswiz/runner/Runner.java`, `src/test/java/com/znsio/teswiz/runner/RunnerMainArgumentsTest.java` (new)
- `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightContextOptionsMapper.java` (new), `PlaywrightJavaDriverManager.java`, `src/test/java/com/znsio/teswiz/web/playwright/PlaywrightContextOptionsMapperTest.java` (new)
- `src/main/java/com/znsio/teswiz/config/browser/PlaywrightBrowserConfigResolver.java`
- `src/main/java/com/znsio/teswiz/visual/PositionIndexParser.java` (new), `businessLayer/visualocr/VisualOcrBL.java`, `screen/visualocr/AbstractVisualOcrScreen.java`
- `src/main/java/com/znsio/teswiz/tools/cmd/CommandLineExecutor.java`
- `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightJavaWebDriver.java`, `PlaywrightJavaCookieMapper.java` (new)
- `src/main/java/com/znsio/teswiz/visual/OcrService.java`, `ImageRecognitionService.java`
- `src/test/java/com/znsio/teswiz/{visual/PositionIndexParserTest, tools/cmd/CommandLineExecutorTest, web/playwright/PlaywrightJavaWebDriverSeleniumApiTest, visual/OcrServiceBehaviourTest, visual/ImageRecognitionServiceBehaviourTest}.java` (new)
- `config/pmd/teswiz-complexity-ruleset.xml` (new), `build.gradle` (`pmd` plugin)
- `.github/workflows/*.yml` (`ubuntu-24.04`; `codeql.yml` `security-and-quality`; unit-test artifact includes the PMD report), `.github/actions/run-and-collect-tests-README.md`
- `docs/index.md`, `docs/getting-started/writing-first-test.md`, `docs/architecture-and-internals/{faqs,ideas-backlog,ocr-and-image-recognition-plan,testng-execution-mode-plan}.md`, `docs/todos/*.md`
- `.codex/skills/teswiz-project/SKILL.md`
- `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`
- `package.json`, `package-lock.json`
- `.github/workflows/Build_And_Run_Unit_Tests_CI.yml` (setup-gradle, debug steps removed)
- `.github/workflows/*.yml` (setup-node v7, setup-java v6; `BrowserStack_PDF_CI.yml` step renamed to Appium 3)
- `.github/actions/cleanup-branch-artifacts/action.yml` (github-script v9)
- `.github/dependabot.yml`
- `docs/getting-started/prerequisites.md`, `docs/getting-started/getting-started-guide.md`
- `docs/architecture-and-internals/playwright-migration-guide.md`
- `docs/engines/mobile-appium-setup.md`, `docs/index.md`, `README.md`
- `CLAUDE.md`, `ANTIGRAVITY.md`
- `Changelog.MD`

## 1.0.56
# Release Notes - teswiz v1.0.56

_Previous release: v1.0.55_

This release makes the `playwright-ts` engine and the configuration file work in consumer projects (outside the teswiz repository), moves the build to Gradle 9.8.2, and picks up the latest Selenium, Playwright, Applitools and AspectJ versions.

## ⚠️ Behaviour Changes

### `HEADLESS`, `IS_FAILING_TEST_SUITE` and `SET_HARD_GATE` are now read from the config file

These three are active properties in the canonical template, but `Setup` only read them from environment variables and system properties, so a value set in the config file was silently ignored. They now fall back to the config file value. Environment variables and system properties still override the file.

If a config file sets one of these to a value you were relying on being ignored (for example `HEADLESS=false` while you expected headless runs), the file value now takes effect. Check your `configs/**/*.properties` before upgrading.

### Deprecated: `web.playwright.PlaywrightTsScreenActionExecutor`

The manual executor in `com.znsio.teswiz.web.playwright` is marked deprecated for removal. Nothing in teswiz uses it: `ScreenRegistry` bridges screen contracts to `*.screen.ts` modules through the executor in `web.playwright.screen`. It also shared its simple name with that executor, which made imports error-prone.

## ✨ Improvements

### `playwright-ts` runs in consumer projects

- **Worker bundled in the jar.** `playwright/worker.mjs` and `worker-provider.mjs` are now packaged under `teswiz/playwright/` in the published jar. Before this, the jar had no worker, so `playwright-ts` could not run outside the teswiz repository.
- **Worker resolution.** The new `PlaywrightWorkerScriptResolver` uses the project's own `playwright/worker.mjs` when present. Otherwise it extracts the bundled worker to `build/teswiz/playwright/`, so Node resolves npm packages from the consumer project's `node_modules`.
- **Readable worker failures.** `PlaywrightWorkerClient` drains the worker's stderr through the new `WorkerStderrTail` (an unread pipe can block the worker). When the worker dies, the error now includes the stderr tail and an install hint instead of only "closed unexpectedly".
- **Consumer screen contracts.** `PlaywrightTsScreenActionExecutor` (in `web.playwright.screen`) treats abstract return types in any package with a `screen` segment as screen contracts, matching `ScreenPackageConvention`. Fluent chains on consumer-namespace contracts no longer return `null`.

### Configurable consumer layout in `build.gradle.sample`

`teswizConfig`, `teswizStepsPackagePath`, `teswizFeaturesDir` and `teswizScreenContractsRoot` can now be set in `gradle.properties`. The defaults keep the `com/znsio/teswiz` layout, and `CONFIG` still overrides `teswizConfig`. The sample also adds a consumer `verifyScreenContracts` task.

### `BROWSER_CONFIG_FILE` added to the canonical template

`BROWSER_CONFIG_FILE` is read by `Setup` but was missing from `teswiz_config.properties.template`. It is now listed (commented) after `WEB_ENGINE`. In `configs/**/*.properties`, existing `BROWSER_CONFIG_FILE` lines moved to their template position with values unchanged, and files that had none got a commented default.

## 📦 Dependency Updates

| Dependency | From | To |
|---|---|---|
| Gradle wrapper | 9.8.1 | 9.8.2 |
| `io.freefair.aspectj.post-compile-weaving` (plugin) | 9.8.2 | 9.8.3 |
| `io.github.ben-manes.versions` (plugin) | 0.66.0 | 0.67.0 |
| `org.aspectj:aspectjrt` | 1.9.25.1 | 1.9.27 |
| `org.seleniumhq.selenium:selenium-java` | 4.51.0 | 4.52.0 |
| `com.microsoft.playwright:playwright` | 1.63.0 | 1.64.0 |
| `com.applitools:eyes-playwright-java5` | 5.86.13 | 5.87.0 |
| `com.applitools:eyes-appium-java5` | 5.79.58 | 5.80.0 |
| `net.bytebuddy:byte-buddy` | 1.18.16 | 1.18.17 |

Notes:

- **Gradle 9.8.2** adds one fix on top of 9.8.1 (a false "graph wasn't stable enough" warning when aligning versions with a virtual platform). If the first `./gradlew` run after upgrading hangs with no output, an earlier 9.8.2 download was probably interrupted. Delete `~/.gradle/wrapper/dists/gradle-9.8.2-bin` and run again.
- **Applitools Eyes 5.80.0 / 5.87.0** declare older Selenium, Appium `java-client` and Playwright versions. teswiz excludes or pins these, so Selenium 4.52.0, `java-client` 11.0.0 and Playwright 1.64.0 are what resolve.

### Not upgraded: Appium driver majors

`appium-uiautomator2-driver` 9.x and `appium-xcuitest-driver` 13.x need `appium ^4.0.0-beta.6`. teswiz is on Appium 3.9.0, so they stay at 8.7.3 and 12.22.8. `.github/dependabot.yml` now ignores semver-major updates for `appium`, `appium-uiautomator2-driver` and `appium-xcuitest-driver` until the Appium 4 migration. Minor and patch updates still come through.

## ✅ Tests

- `PlaywrightTsScreenActionExecutorTest`: covers framework contracts, consumer contracts, and non-screen types next to or outside a `screen` package. Uses new `com.example.consumer` fixtures (`CheckoutScreen`, `CheckoutSummary`, `OrderLine`).
- `SetupConfigFileBooleansTest`: config file value, default, and env/system-property override for `HEADLESS`, `IS_FAILING_TEST_SUITE` and `SET_HARD_GATE`.
- `PlaywrightWorkerScriptResolverTest`: covers resolver precedence, extraction, refresh, and the missing-bundle error.
- `PlaywrightWorkerClientDiagnosticsTest`: worker stderr is included in the failure message.
- `BuildGradleSampleTest`: pins the `gradle.properties` layout properties, their defaults, and the `verifyScreenContracts` task.
- `./gradlew build` passes on Gradle 9.8.2 with the updated dependencies, including `validateConfigurationTemplates`.

## 📝 Docs

- `docs/getting-started/getting-started-guide.md`, `docs/getting-started/prerequisites.md`: the `gradle.properties` layout, and Node 22.6+ for `playwright-ts`.
- `docs/architecture-and-internals/playwright-migration-guide.md`: consumer `verifyScreenContracts`, plus a new section on running `playwright-ts` outside the teswiz repository.
- `docs/configuration/configuration-parameters.md`: `HEADLESS` precedence (env / system property over config file).
- `docs/architecture-and-internals/breaking-changes.md`: new 1.0.47–1.0.55 run-notifications section (footer signature change, Slack/email output changes, humanised labels) and a 1.0.49 API section (null fields sent, no double URL encoding).
- `docs/architecture-and-internals/architecture-notes.md`: `reporting.notify.failure` reflects the `HUMANIZED_TYPE` default label, the `SIMPLE_TYPE` opt-out, and the Outlook-safe HTML breakdown table.

## 🛠 Changed Files Summary

- `src/main/java/com/znsio/teswiz/runner/Setup.java`
- `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightTsScreenActionExecutor.java`
- `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightWorkerClient.java`
- `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightWorkerScriptResolver.java` (new)
- `src/main/java/com/znsio/teswiz/web/playwright/WorkerStderrTail.java` (new)
- `src/main/java/com/znsio/teswiz/web/playwright/screen/PlaywrightTsScreenActionExecutor.java`
- `src/test/java/com/example/consumer/**` (new fixtures)
- `src/test/java/.../BuildGradleSampleTest.java`, `SetupConfigFileBooleansTest.java`, `PlaywrightWorkerClientDiagnosticsTest.java`, `PlaywrightWorkerScriptResolverTest.java`, `PlaywrightTsScreenActionExecutorTest.java` (new)
- `configs/teswiz/teswiz_config.properties.template` and `configs/**/*.properties` (`BROWSER_CONFIG_FILE`)
- `build.gradle` (dependency and plugin versions, bundled worker, `wrapper` 9.8.2, version 1.0.56)
- `build.gradle.sample`
- `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`
- `.github/dependabot.yml`
- `docs/**` (see Docs above)
- `Changelog.MD`

## 1.0.55
# Release Notes - teswiz v1.0.55

_Previous release: v1.0.54_

This release makes the failure-breakdown "Failure" column read as plain English for every exception, renders the email table responsively in every mail client (including Outlook), and drops the parentheses around the raw class name.

## ✨ Improvements

### Failure column is humanised by default
`FailureBreakdownRenderer` gains `HUMANIZED_TYPE` and makes it the default label for the no-argument `slack(...)` / `html(...)` overloads (the old pass-through is still available as `SIMPLE_TYPE`). A clean `CamelCase` class name has a trailing `Exception`/`Error` dropped, is split into words and sentence-cased, so `FreeSpinNotAwardedException` reads `Free spin not awarded` instead of showing the raw class name. A name that is not a plain letters-only identifier (it carries a digit or any of `& < >` or a backtick), a blank type and the `Unclassified` marker are all left untouched. A consumer still passes its own `Function<String,String>` to override specific types, composing it over `HUMANIZED_TYPE`.

### Responsive email table in every client (Outlook included)
The HTML failure-breakdown table now declares its full width and fixed layout **inline** on the `<table>` (`width="100%" style="width:100%;table-layout:fixed"`) with a `<colgroup>` of percentage column widths (22% / 33% / 33% / 12%), because Outlook's Word engine ignores `<style>`-block table and column widths and would otherwise shrink the table to its content. The percentages scale the table with the email card at any viewport. The `<style>` rules are reduced to progressive enhancement (word-break, count alignment) for clients that honour them.

### Raw class moves under the location, no parentheses
In the "Where" column the raw exception class is no longer wrapped in parentheses beside the location. In the email it sits on its own line below the location (`RewardsBL.awardFreeSpins:113` then `IndexOutOfBoundsException`); in Slack it follows the location on the same line without parentheses.

## ✅ Tests
- `FailureBreakdownRendererTest` / `FailureBreakdownSlackLayoutTest` updated to the humanised label, the inline full-width table + colgroup, and the no-parentheses two-line "Where"; new cases pin the humaniser (suffix strip, CamelCase split, blank/`Unclassified`/non-identifier left as-is) and the inline responsive-table markup. Full `com.znsio.teswiz.reporting.notify.*` suite green.

## 1.0.54
# Release Notes - teswiz v1.0.54

_Previous release: v1.0.53_

This release fixes the layout of the failure-breakdown table in the notification email so its columns no longer get clipped.

## 🐛 Fixes

### Email failure-breakdown table is responsive (no truncation)

In the end-of-run notification email, the failure-breakdown table (Failure, Where, Failing step, Count) could clip the "Where" and "Failing step" columns, because the generic two-column `th{width:40%}` rule — meant for key/value tables — also applied to it. The first column took 40% and the long values overflowed the 680px card.

- `HtmlReport` now carries a dedicated `table.breakdown` rule set: `table-layout:fixed` with `word-break` / `overflow-wrap` so a long place (`Class.method:line`) or Gherkin step wraps instead of overflowing, and a narrow, non-wrapping `Count` column. The table fits the card with no horizontal scroll.
- `FailureBreakdownRenderer` tags the table `class="breakdown"` and the count cells `class="count"` so those rules apply.

The Slack rendering and the content of the breakdown are unchanged; this is email styling only.

## ✅ Tests

- `FailureBreakdownRendererTest` updated for the new markup (the `breakdown` table class and `count` cell class).

## 🛠 Changed Files Summary

- `src/main/java/com/znsio/teswiz/reporting/notify/HtmlReport.java`
- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownRenderer.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownRendererTest.java`

## 1.0.53
# Release Notes - teswiz v1.0.53

_Previous release: v1.0.52_

This release adds the **failing Gherkin step** to the failure breakdown and shows exception types through a **friendly label**, so the "why did things fail?" block reads for both testers and engineers (GAMC-20840). It also picks up the latest Applitools SDK versions.

## ✨ Added

### Failing step as a third grouping dimension

`FailureCause` now carries a third component, `step`, alongside `type` and `location`. `FailureClassifier` reads the Gherkin step that was running when a scenario failed:

- from the `[step N: ...]` marker a soft-assertion (`AssertJMultipleFailuresError`) message carries, or
- from the Cucumber `at ✽.<step>(file:...feature:n)` pseudo-frame in an ordinary failure's stack trace.

When neither source carries the step (a truncated trace, an error with no step line, an unclassified failure), the step is the empty string and the cause degrades to the previous `(type, location)` grouping, so the change is backward compatible. The breakdown tally key widens to `(type, location, step)`, so one code location reached by several steps splits into one row per step with its own count, and the per-step counts still sum to `total()`.

### Friendly exception label (consumer-supplied)

`FailureBreakdownRenderer.html` and `.slack` take an optional `Function<String,String>` that maps an exception's simple class name to a human-readable label (for example `PoolExhaustedException` → "No test user available"). The overloads without it default to `SIMPLE_TYPE` (the class name unchanged), so existing callers are unaffected. The raw class name is always shown next to the code location, so an engineer still has the exact type to search for.

### Grouped, labelled rendering

The breakdown now renders grouped by `(type, location)`:

- **Slack**: a two-line group header (`×N` count and friendly label, then `location · RawClass`), with one nested `└ step  ×N` row per failing step. Splitting the location and class onto their own line keeps each line short so a narrow Slack client does not wrap and misalign the block.
- **Email**: a four-column table — Failure (friendly label), Where (location with the raw class beside it), Failing step, Count — with a group header row and a nested row per step.
- Counts are labelled `×N` in both channels; the HTML "Total failed" row keeps a plain number.

## ⬆️ Dependency Updates

Applitools SDK bumps:

| Dependency | From | To |
| :--- | :--- | :--- |
| `com.applitools:eyes-images-java5` | 5.83.15 | 5.83.16 |
| `com.applitools:eyes-appium-java5` | 5.79.57 | 5.79.58 |
| `@applitools/eyes-playwright` | 1.51.4 | 1.51.5 |

## ✅ Tests

- `FailureClassifierTest`: step read from a soft-assertion message marker, from a Cucumber trace frame, and absent when neither is present; `label()` joins type, place and step.
- `FailureBreakdownRendererTest` / `FailureBreakdownSlackLayoutTest`: grouped rendering, the nested per-step rows and counts, the friendly label, the `×N` count mark in both channels, and the existing escaping and alignment guarantees still hold.
- `FailureBreakdownTest`: two failures at the same `(type, location)` on different steps are two causes; a blank step degrades to the old single-row behaviour.

## 📝 Docs

- `docs/subsystems/run-notifications.md`: the failure breakdown section documents the `step` component, the `FailureCause(type, location, step)` shape, the `slack`/`html` friendly-label overloads, the grouped two-line Slack and four-column HTML output, and an upgrade note from 1.0.52. The flow diagram node reflects the new grouping and label.
- `docs/architecture-and-internals/architecture-notes.md`: the `reporting.notify.failure` package description now covers the step component, per-step cause splitting, the grouped Slack/HTML rendering, and the optional friendly-label function.

## 🛠 Changed Files Summary

- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureCause.java`
- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureClassifier.java`
- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdown.java`
- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownRenderer.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureClassifierTest.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownTest.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownRendererTest.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownSlackLayoutTest.java`
- `docs/subsystems/run-notifications.md`
- `docs/architecture-and-internals/architecture-notes.md`
- `build.gradle` (Applitools bumps, version 1.0.53)
- `package.json` / `package-lock.json` (Applitools Playwright bump)
- `Changelog.MD`

## 1.0.52
# Release Notes - teswiz v1.0.52

_Previous release: v1.0.51_

This release makes the Slack "why did things fail?" block survive Slack markup in an exception type or place, so a failure thrown from a constructor keeps its `<init>`.

## 🐛 Fixes

### 🔒 Slack failure breakdown escapes `&`, `<` and `>`

Slack reads `&`, `<` and `>` as markup everywhere in a message, even inside a triple-backtick code block. A failure thrown from a constructor has a place like `Widget.<init>:7`, and Slack read `<init>` as a mention and dropped it, so the row showed `Widget.:7`.

`FailureBreakdownRenderer.slack(...)` now escapes `&`, `<` and `>` to their HTML entities (`&amp;`, `&lt;`, `&gt;`), folding in the existing backtick-to-`'` handling.

- The escaping is applied **after** the fixed-width padding, so the monospaced columns still line up. Each entity renders as a single glyph in Slack, so the visible table is unchanged.
- The same escaping is applied to the `_+K more causes in the full report_` line.
- `html(breakdown)` is unchanged; it already escaped via `HtmlReport.escape`.

Previously consumers worked around this by escaping teswiz's Slack block themselves; they can now call `FailureBreakdownRenderer.slack(...)` directly.

## ✅ Tests

New `FailureBreakdownSlackLayoutTest` cases cover:

- a constructor-frame place (`Widget.<init>:7`) keeping its `<init>` as `Widget.&lt;init&gt;:7`,
- `&` and `>` in an exception type being escaped,
- alignment holding after escaping (padding happens on the raw text, so an escaped row lines up with a plain row),
- the `+K more causes` line being escaped too.

## 📝 Docs

- `docs/subsystems/run-notifications.md`: the "Rendered for Slack" section now explains that `&`, `<` and `>` are escaped to entities and why, using the constructor-frame case as the example.

## 🛠️ Changed Files Summary

- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownRenderer.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownSlackLayoutTest.java`
- `docs/subsystems/run-notifications.md`
- `Changelog.MD`
- `build.gradle` (version 1.0.52)

## 1.0.51
# Release Notes - teswiz v1.0.51

_Previous release: v1.0.50_

This release makes the Slack "why did things fail?" block read as an aligned table and caps it at the top causes. It also documents the CI job-permission rule for workflows.

## 🚀 Enhancements

### 📊 Slack failure breakdown is an aligned table, capped at the top causes

`FailureBreakdownRenderer.slack(...)` now renders the failure breakdown so it reads as a table in Slack:

- **Heading with counts:** `*Why did things fail?*  8 failures · 3 causes`. The wording is singular for one (`1 failure · 1 cause`).
- **Aligned rows in a code block:** each row is the count (right-aligned), the exception type (padded to the longest type shown) and the place, biggest cause first. The code block makes Slack show it monospaced, so the columns line up.
- **Top causes only:** at most `DEFAULT_MAX_CAUSES` (10) causes are listed, followed by `_+K more causes in the full report_`. The new `slack(breakdown, maxCauses)` overload changes the limit.
- **Safe output:** a backtick in a type or place is printed as `'`, so it can't close the code block early.
- `html(breakdown)` is unchanged.

Rendered in Slack:

~~~~text
*Why did things fail?*  8 failures · 3 causes
```
6  IndexOutOfBoundsException     RewardsBL.awardFreeSpins:113
1  AssertJMultipleFailuresError  BalanceTracking.assertReduced:111
1  TargetClosedError             GamePlayScreenPlaywrightJava.spin:396
```
~~~~

## ⚠️ Upgrading from 1.0.50

The Slack text has changed. In 1.0.50 it was `*Why did things fail? (N)*` followed by one `Type @ Place  count` line per cause, with every cause listed. If your composer or its tests match the old text, update them. If you need more than 10 causes, pass a larger limit with `slack(breakdown, maxCauses)`.

## ✅ Tests

- New `FailureBreakdownSlackLayoutTest` (7 tests) covers the heading counts and singular wording, right-aligned counts, column padding to the causes shown, the default top-10 limit and the configurable limit, the "+K more causes" line, and backtick handling.
- `FailureBreakdownRendererTest` now covers only the HTML table and empty output. Its old Slack-layout test was removed, because the new class pins the new layout.

## 📝 Docs

- `docs/subsystems/run-notifications.md`: new Slack example and behaviour, an upgrade note for the layout change, and the flow diagram now shows the top-N Slack rendering.
- `docs/architecture-and-internals/architecture-notes.md`: the `reporting.notify.failure` package is now described in the package notes.
- `.github/actions/run-and-collect-tests-README.md`: new "Job Permissions" section. Every job declares `permissions:`, with `contents: read` by default and `actions: write` only when it uses `cleanup-branch-artifacts`.
- `Changelog.MD`: the 1.0.50 entry now includes the CI token permissions fix and the dependency updates that shipped in it, and its security table renders correctly.

## 🛠️ Changed Files Summary

- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownRenderer.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownSlackLayoutTest.java` (new)
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownRendererTest.java`
- `docs/subsystems/run-notifications.md`
- `docs/architecture-and-internals/architecture-notes.md`
- `.github/actions/run-and-collect-tests-README.md`
- `Changelog.MD`

## 1.0.50
# Release Notes - teswiz v1.0.50

_Previous release: v1.0.49_

This release adds a "why did things fail?" breakdown to the end-of-run notification content, so a Slack or email notification can group failures by cause instead of just counting them. It also resolves all critical and high npm security alerts, tightens CI token permissions, and picks up patch and minor dependency updates.

## 🚀 Enhancements

### 📊 Failure type, stack trace, and a failure breakdown in notification reports

- **`JUnitResults.Scenario` gains `type` and `trace`.** `type` is the fully-qualified exception class, read from the `type` attribute of the JUnit `<failure>` / `<error>` element; `trace` is its stack trace body. Both are `""` when the runner wrote none. The 4-argument `Scenario` constructor is kept for a scenario with no recorded exception (a passed or skipped one), so existing callers are unaffected.
- **Why this is needed:** the existing `reason` rarely names the exception on its own — an uncaught exception's message is just its text (`Index 0 out of bounds for length 0`) — and an XML parser collapses the line breaks inside the attribute into spaces.
- **New `com.znsio.teswiz.reporting.notify.failure` package** turns failing scenarios into causes:
  - **`FailureClassifier.classify(scenario)`** returns a `FailureCause`: the simple exception class name, plus the place it came from — the first stack frame belonging to the code under test, with JDK and well-known library frames (JUnit, Cucumber, AssertJ, Playwright, Selenium, RestAssured, Appium, and more) skipped. For an AssertJ soft-assertion wrapper (`AssertJMultipleFailuresError`, thrown from the after-scenario hook, whose own stack points at the framework), the place comes from the assertion's own origin line in the message instead.
  - **`FailureCause(type, location)`** — two scenarios with an equal cause failed for the same reason, so eight failures on one line read as one cause, not eight entries.
  - **`FailureBreakdown`** — `JUnitResults.failureBreakdown()` or `FailureBreakdown.of(scenariosByFeature)` tallies failed and errored scenarios by cause, biggest first. `total()` always equals `failed() + errors()`: an unclassifiable failure is counted as `Unclassified`, never dropped. `plus(other)` merges breakdowns across several runs.
  - **`FailureBreakdownRenderer`** — `slack(breakdown)` renders a bold heading with the total and one line per cause (`Type @ Class.method:line  count`); `html(breakdown)` renders a three-column table (exception type, where, count) with a total row. Both return `""` for an empty breakdown, so a green run carries no block.
- Covered by new `JUnitResultsFailureDetailsTest`, `FailureClassifierTest`, `FailureBreakdownTest`, and `FailureBreakdownRendererTest`, plus a `cucumber-junit-failure-causes.xml` fixture.

```text
*Why did things fail? (8)*
  IndexOutOfBoundsException @ RewardsBL.awardFreeSpins:113  6
  AssertJMultipleFailuresError @ BalanceTracking.assertReduced:111  1
  TargetClosedError @ GamePlayScreenPlaywrightJava.spin:396  1
```

## 🔒 Security

Resolves the critical and high Dependabot alerts on transitive npm dependencies in `package-lock.json`. All of them come in through the Appium toolchain; none are in the Java framework.

| Package | Advisory | Before | After | How |
| :--- | :--- | :--- | :--- | :--- |
| `shell-quote` | Critical: `quote()` command injection via a line terminator | 1.10.0 (bundled in `appium-xcuitest-driver`) | 1.12.0 | `appium-xcuitest-driver` 12.20.0 → 12.22.8; override 1.11.0 → 1.12.0 |
| `node-forge` | High: RSA PKCS#1 v1.5 signature verification, plus several TLS and PEM DoS issues | 1.4.0 | 1.4.3 | override bumped |
| `multer` | High: DoS via a pending asynchronous `fileFilter` | 2.3.0 | 2.5.0 | override bumped |
| `body-parser` | High: DoS via trailing bytes after a compressed body (2 advisories) | 2.3.0 (bundled), 1.20.8 | 2.4.0, 1.20.9 | `appium-xcuitest-driver` bump; scoped overrides for `@appium/base-driver` and `express@4.22.2` |
| `follow-redirects` | High: sensitive headers and custom credentials can follow cross-realm redirects | 1.16.1 | 1.16.2 | new override |
| `qs` | Moderate: array-limit bypass, `isBuffer` DoS, `stringify` comma-format DoS (3 advisories) | 6.15.3 | 6.17.1 | override bumped |
| `fast-uri` | Moderate: scheme spoofing, host confusion, mailto header injection, host case normalisation (4 advisories) | 4.1.4 | 4.2.3 | override bumped |
| `js-yaml` | Moderate: `maxTotalMergeKeys` does not limit CPU for empty merge sources | 5.2.2 | 5.5.0 | override bumped |

- `appium-xcuitest-driver` moves within the 12.x line (12.20.0 → 12.22.8) and keeps the same `appium ^3` peer range. Its bundled `shell-quote` and `body-parser` copies can't be changed with npm `overrides`, so the driver bump is what fixes them.
- `body-parser` 1.x (used by `local-cors-proxy`'s Express 4) is pinned with a scoped override, so it is not forced onto 2.x.
- `npm audit` after the change: **0 critical** (was 2), **0 high** (was 5), 36 moderate, 1 low (49 → 37 total).
- **No fix available yet:**
  - `glob` (moderate, AbortSignal bypass) and `minimatch` (moderate, globparts DoS): every patched version is a major bump (`glob` 14.0.2+, `minimatch` 11.0.1+). The vulnerable copies are also bundled inside `appium-uiautomator2-driver` and `appium-xcuitest-driver`, which overrides can't reach, and the latest `@appium/support` (7.2.9) still pins `glob` 13.0.6. These need an upstream Appium release.
  - `request` (moderate, SSRF): deprecated, and 2.88.2 is the final release. It is pulled in only by `appium-device-farm` → `local-cors-proxy`.
  - Most of the remaining moderate count is npm flagging the parents of these three packages.

### CI token permissions

`API_Engines_CI.yml` and `InteractiveCLI_CI.yml` now declare `permissions: contents: read` for their jobs, instead of inheriting the repository's default token permissions. This resolves the CodeQL "Workflow does not contain permissions" alerts.

## ⬆️ Dependency Updates

**Java (Gradle), patch and minor releases:** `rest-assured` 6.2.0 → 6.3.0, `cucumber-reporting` 5.11.0 → 5.12.0, `eyes-images-java5` 5.83.13 → 5.83.15, `eyes-appium-java5` 5.79.55 → 5.79.57, `jakarta.mail` 2.0.1 → 2.0.2, `okhttp` 5.5.0 → 5.5.1, `byte-buddy` 1.18.15 → 1.18.16, and the optional `onnxruntime` 1.31.0 → 1.31.1 and `google-auth-library-oauth2-http` 1.54.0 → 1.55.0.

**npm:** `appium-xcuitest-driver` 12.20.0 → 12.22.8, `appium-uiautomator2-driver` 8.7.2 → 8.7.3, `@appium/relaxed-caps-plugin` 3.0.1 → 3.0.2, `@applitools/eyes-playwright` 1.51.2 → 1.51.4. The Appium drivers stay on the lines that support Appium 3; `appium-uiautomator2-driver` 9.x and `appium-xcuitest-driver` 13.x require Appium 4, which is still in beta.

## 📝 Docs

- `docs/subsystems/run-notifications.md`: `scenariosByFeature()` documented with the new `type` and `trace` fields; new "Failure breakdown" section with the type table and a rendered Slack example; flow diagram updated to show `failure.FailureBreakdown` / `FailureBreakdownRenderer` feeding off `JUnitResults`.

## 🛠️ Changed Files Summary

- `src/main/java/com/znsio/teswiz/reporting/notify/JUnitResults.java`
- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureCause.java` (new)
- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureClassifier.java` (new)
- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdown.java` (new)
- `src/main/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownRenderer.java` (new)
- `src/test/java/com/znsio/teswiz/reporting/notify/JUnitResultsFailureDetailsTest.java` (new)
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureClassifierTest.java` (new)
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownTest.java` (new)
- `src/test/java/com/znsio/teswiz/reporting/notify/failure/FailureBreakdownRendererTest.java` (new)
- `src/test/resources/notify/cucumber-junit-failure-causes.xml` (new)
- `.github/workflows/API_Engines_CI.yml`
- `.github/workflows/InteractiveCLI_CI.yml`
- `docs/subsystems/run-notifications.md`
- `build.gradle` (dependency updates)
- `package.json` (Appium driver bumps, security overrides)
- `package-lock.json` (regenerated)

## 1.0.49
# Release Notes - teswiz v1.0.49

_Previous release: v1.0.48_

This release makes the RestAssured and Playwright API engines send the same request, so switching `API_ENGINE` no longer changes what goes on the wire.

## ⚠️ Behaviour change to check

If a test relied on the Playwright engine **omitting** a `null` field from a `Map` or `Collection` body, that field is now sent (`{"amount":null}`). Remove the key from the map to leave it out.

## 🐞 Fixes

### 🔁 Identical requests on every API engine

**Request bodies.** A `Map` or `Collection` body is now written by one shared Jackson serialiser (`JsonRequestBody`) on both engines:

- Explicit `null` fields and `null` list elements are kept. The Playwright path used to drop them because it went through org.json.
- Decimals stay decimals (`1.0`, not `1`), and key order is preserved.
- Nested `JSONObject`, `JSONArray` and `JSONObject.NULL` values are written as the JSON they hold.
- A body that cannot be written as JSON fails fast with `InvalidTestDataException`.

**URLs on RestAssured.** A URL you pass is treated as already encoded (`RequestUrl`):

- A valid `%XX` escape is no longer encoded a second time. `%3A` used to be sent as `%253A`.
- Only characters that are illegal in a URL (a space, a non-ASCII character, a stray `%`) are percent-encoded.
- `queryParams` map values are encoded once, and a `null` value is skipped, as on Playwright.

A space in a `queryParams` value may go out as `+` (Playwright) or `%20` (RestAssured). Both decode to the same value on the server.

### `playwright-ts` for `API_ENGINE` is the same client as `playwright-java`

`API_ENGINE=playwright-ts` has always been routed to the Playwright Java `APIRequestContext` client. There is no separate TypeScript API worker. The docs said otherwise and now say so correctly, so the request fix above applies to `playwright-ts` with no extra change.

## ✅ Tests

- New `ApiEngineRequestContractTest` records the raw request on a local HTTP server and runs 9 contracts against each of `rest-assured`, `playwright-java` and `playwright-ts` (27 cases). It covers null fields at every level, decimals, collection bodies, empty and `null` bodies, `null` query maps, already-encoded URLs, reserved characters in `queryParams`, an unencoded space in a URL, and org.json values inside a map.

## 📝 Docs

- `docs/engines/running-api-tests.md`: new "What goes on the wire (same for every engine)" section, including `JSONObject` / `JSONArray` bodies passed directly. The `playwright-ts` description and engine diagram are corrected.
- `docs/architecture-and-internals/architecture-notes.md`: new `com.znsio.teswiz.api` package section with a diagram, a package rule for shared request shaping, and the architecture diagram corrected for `playwright-ts`.
- `docs/architecture-and-internals/playwright-migration-guide.md`: note that the request is the same on every engine and what to do if a test depended on a dropped `null` field.
- `README.md`, `docs/configuration/configuration-parameters.md`, `docs/getting-started/configuring-test-execution.md`, `docs/examples/api-example.md`: `playwright-ts` API wording corrected.

## 🛠️ Changed Files Summary

- `src/main/java/com/znsio/teswiz/api/JsonRequestBody.java` (new)
- `src/main/java/com/znsio/teswiz/api/RequestUrl.java` (new)
- `src/main/java/com/znsio/teswiz/api/RestAssuredApiEngineClient.java`
- `src/main/java/com/znsio/teswiz/api/PlaywrightApiEngineClient.java`
- `src/test/java/com/znsio/teswiz/services/ApiEngineRequestContractTest.java` (new)
- `docs/engines/running-api-tests.md`
- `docs/architecture-and-internals/architecture-notes.md`
- `docs/architecture-and-internals/playwright-migration-guide.md`
- `docs/configuration/configuration-parameters.md`
- `docs/getting-started/configuring-test-execution.md`
- `docs/examples/api-example.md`
- `README.md`
- `Changelog.MD`

## 1.0.48
# Release Notes - teswiz v1.0.48

_Previous release: v1.0.47_

This release adds engine-agnostic body accessors to `TeswizApiResponse`, so API consumers can map a response body without reaching for a specific HTTP engine's types. It also makes `API_MAX_RETRIES` work from `config.properties`.

## 🚀 Enhancements

### 🧩 Typed JSON mapping and XML reading on `TeswizApiResponse`

`TeswizApiResponse` previously exposed only `asJsonObject()` / `asJsonArray()` (org.json). Four additive accessors now cover the common "map the body to my own type" and "read an XML supplier" needs. All of them work off the raw response-body string, so they behave the same whether the RestAssured or Playwright engine produced the response.

- **`as(Class<T>)`**: map the JSON body to a caller-supplied type via Jackson.
- **`as(TypeReference<T>)`**: map the JSON body to a generic type such as `List<Foo>`.
- **`asXmlDocument()`**: parse the body as an XXE-safe `org.w3c.dom.Document`. External entity resolution and DOCTYPE are disabled because the body is untrusted server data.
- **`xmlString(xpath)`**: evaluate an XPath against the XML body. Returns the matched text, or an empty string when the path matches nothing.

A malformed body fails fast with `InvalidTestDataException` naming the target type or XPath. The change is purely additive: no existing method, engine, or consumer changes. Covered by a new `TeswizApiResponseTest`.

```java
TeswizApiResponse response = ApiService.get(baseUrl, queryParams);

Forecast forecast = response.as(Forecast.class);
List<Forecast> forecasts = response.as(new TypeReference<List<Forecast>>() {});
String city = response.xmlString("/response/location/city");
```

## 🐞 Fixes

### 🔁 `API_MAX_RETRIES` can now be set in the config file

The Playwright API engine retries each call up to `API_MAX_RETRIES` times (default 3). Until now the key was only read from an environment variable or `-D` system property, so a value in `config.properties` was silently ignored, even though the docs and config template listed it as a config key.

It now resolves like the other numeric keys:

1. environment variable / `-D` system property
2. `config.properties`
3. default of 3

The value is never below 1 (`Runner.getApiMaxRetries()`). Consumers whose API calls are not safe to resend (for example wallet bet/win/void POSTs) can pin `API_MAX_RETRIES=1` in their config to fail fast on the first 502/503/504 or exception. Default behaviour is unchanged. Covered by a new `ApiMaxRetriesConfigTest`.

## 📝 Docs

- `docs/engines/running-api-tests.md`: the diagram now ends in `TeswizApiResponse` and its accessors, a new "Reading the response body" section covers the typed JSON and XML accessors, and the `API_MAX_RETRIES` precedence is documented.
- `docs/configuration/configuration-parameters.md`: `API_MAX_RETRIES` precedence and minimum documented.
- `docs/subsystems/run-notifications.md`: flow diagram now shows `JUnitResults`, `HtmlReport` and the merged-config fallback. A new "Report content helpers" section documents the scenario breakdown, run duration, generic reproduce command, and an upgrade note for the 1.0.47 `footer` / `slackFooter` signature change.
- `docs/architecture-and-internals/architecture-notes.md`: notification helper and `RunEnvironment` descriptions brought in line with 1.0.47.

## 🛠️ Changed Files Summary

- `src/main/java/com/znsio/teswiz/api/TeswizApiResponse.java`
- `src/main/java/com/znsio/teswiz/api/PlaywrightApiEngineClient.java`
- `src/main/java/com/znsio/teswiz/runner/Runner.java`
- `src/main/java/com/znsio/teswiz/runner/Setup.java`
- `src/test/java/com/znsio/teswiz/api/TeswizApiResponseTest.java`
- `src/test/java/com/znsio/teswiz/runner/ApiMaxRetriesConfigTest.java`
- `docs/engines/running-api-tests.md`
- `docs/configuration/configuration-parameters.md`
- `docs/subsystems/run-notifications.md`
- `docs/architecture-and-internals/architecture-notes.md`
- `docs/getting-started/getting-started-guide.md`
- `Changelog.MD`
- `build.gradle` (version 1.0.48)

## 1.0.47
# Release Notes - teswiz v1.0.47

_Previous release: v1.0.46_

This release builds on the generic end-of-run notification engine shipped in 1.0.46, making the notification content richer and the run-provenance resolution cleaner, more generic, and deterministic.

## 🚀 Enhancements

### 📊 Full per-scenario breakdown + run duration in notification reports

The notification content model now exposes the complete per-scenario detail of a run, not just the failure summary, so a Slack/email notification can render the whole result.

- **`JUnitResults.scenariosByFeature()`** — every scenario grouped by feature name, each carrying its status, reason, and time. Backed by a new `ScenarioStatus` enum and `Scenario` / `FeatureScenarios` records.
- **`JUnitResults.durationSeconds()`** — the total run duration parsed from the JUnit XML.
- **`failuresByFeature()` is now derived from the same scenario list**, so the existing API is unchanged for current callers — this is additive.
- **`HtmlReport` rendering helpers** — `scenarioTable` (an email status-pill table), `slackScenarios` (a Slack glyph blockquote), and `formatDuration`, plus the pill/table CSS to style them.
- Covered by a new `JUnitResultsTest` and a mixed JUnit fixture exercising status, reason, time, and feature grouping.

## 🔧 Refactors

### Reproduce-command made generic — no domain keys in `RunEnvironment`

`RunEnvironment` no longer builds or carries a "reproduce command". It previously held a hardcoded list of consumer run-input keys (`REGION`, `MARKET`, `BRAND`, `INTEGRATION`, `GAME`, `TAG`, `CONFIG`, `TARGET_ENVIRONMENT`) — domain coupling teswiz should not own.

- Dropped the `reproduceCommand` field, the key list, and its builder from `RunEnvironment`.
- Added a generic **`HtmlReport.reproduceCommand(Map<String,String> orderedInputs)`** that POSIX-shell-quotes a caller-supplied ordered map into a `K=V ... ./gradlew run` string — bare when safe, single-quoted (with `'\''` escaping) when a value has spaces or shell specials.
- `footer(env, cmd)` / `slackFooter(env, cmd)` now take the command as a parameter, so the consuming project composes it and teswiz only renders and shell-escapes.
- The reproduce-command tests moved out of `RunEnvironmentTest` into a new `HtmlReportTest` (ordering, quoting, embedded-quote, blank-skip, empty-map).
- Fixes the `zsh: unknown sort specifier` error that a Cucumber `TAG` containing parentheses caused when the old, unquoted command was pasted into a shell.

## 🐞 Fixes

### Notification platform/environment resolved from merged config, deterministically

- `RunEnvironment` now resolves `PLATFORM` / `TARGET_ENVIRONMENT` from the injected process environment first, falling back to an **injected configuration lookup** rather than reaching into a global configuration singleton. On a real run the reporter wires this to teswiz's merged configuration (`Setup::getFromConfigs`), so a local run still picks up values from `config.properties`.
- Because the fallback is injected rather than read from global static state, resolution is now deterministic and unit-testable offline — removing an order-dependent unit-test failure where another test populating the global config singleton would leak a stale `TARGET_ENVIRONMENT` value into `RunEnvironment`.
- The 4-arg `RunEnvironment.resolve(...)` keeps its signature and now holds no dependency on global state; a new 5-arg overload takes the explicit config lookup for the real run.

## 📝 Docs

- `docs/subsystems/run-notifications.md` updated to note that `RunEnvironment` platform/environment fall back to resolved configuration.

## 🛠️ Changed Files Summary

- `src/main/java/com/znsio/teswiz/reporting/notify/JUnitResults.java`
- `src/main/java/com/znsio/teswiz/reporting/notify/HtmlReport.java`
- `src/main/java/com/znsio/teswiz/reporting/notify/RunEnvironment.java`
- `src/main/java/com/znsio/teswiz/reporting/notify/RunNotifications.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/JUnitResultsTest.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/HtmlReportTest.java`
- `src/test/java/com/znsio/teswiz/reporting/notify/RunEnvironmentTest.java`
- `src/test/resources/notify/cucumber-junit-mixed.xml`
- `docs/subsystems/run-notifications.md`
- `build.gradle` (version bump to 1.0.47)

## 1.0.46
# Release Notes - teswiz v1.0.46

_Previous release: v1.0.45_

## 🚀 Key Highlights & Enhancements

### 🔔 New: generic end-of-run notification engine (`com.znsio.teswiz.reporting.notify`)

teswiz now owns the entire test-run notification runtime — Slack + email channels, delivery, gating, and the **end-of-run trigger** — so any consumer gets run-notifications by supplying only the message content. teswiz's `Runner` fires it itself after the cucumber report is written; a consumer configures a composer and never wires a Gradle task.

- **`RunNotifications.fireIfConfigured(...)`** — the entry point teswiz's `Runner` calls after `cucumber-junit-report.xml` is complete and before JVM exit. Loads the notifications file named by the `NOTIFICATIONS_CONFIG_FILE` config key (same file-path convention teswiz uses for `ENVIRONMENT_CONFIG_FILE` / `TEST_DATA_FILE`; unset = notifications off), resolves the consumer's composer via `notify.composer.class`, builds a `RunResult` (JUnit XML from `LOG_DIR/reports/`, counts, run environment), and delivers via `Notifier`. A no-op when no composer is configured; every failure is logged and swallowed so a notifier problem never changes a run's result. The run branch is resolved from `CI_COMMIT_REF_NAME` else teswiz's already-resolved `Setup.BRANCH_NAME`.
- **`RunNotificationComposer` (SPI) + `Notification` / `RunResult`** — the consumer implements `compose(RunResult) → List<Notification>`; a `Notification` carries a free-form type key, subject, Slack text, HTML body, and resolved per-channel destinations. teswiz never branches on the type key.
- **`NotificationSettings`** — generic loader: reads a `.properties` resource and layers an environment override on every key (env wins; blank/absent env falls through to the file), mapping `notify.enabled` → `NOTIFY_ENABLED`. Secrets are read from the environment only, never the file.
- **`Notifier`** — applies the master switch (`notify.enabled`) + branch allowlist (`notify.branches`) gate, then sends each `Notification` to the channels whose destination is present; returns a per-target `NotificationOutcome` map and never throws.
- **`SlackSender`** — POSTs `{"text":...}` to an incoming webhook; **`EmailSender`** sends self-contained HTML over SMTP via `jakarta.mail` with bounded 15s timeouts and an unauthenticated-relay path. Both never throw.
- **Content helpers** — `JUnitResults` (parse JUnit XML → counts + failures-by-feature), `HtmlReport` (status card / footer / links / Slack separator / escape), `LlmUsageLog` (fail-soft parse of teswiz's end-of-run "LLM Vision Usage Summary"), `RunEnvironment` (run provenance). `BranchGate` + `NotificationChannel` + `NotificationOutcome` round out the model; `com.znsio.teswiz.exceptions.NotificationException` surfaces a broken config.
- **CI-provider-agnostic run provenance** — a new `CiProvider` detects the CI system from its signature environment variable (GitLab, GitHub Actions, Jenkins, CircleCI, Bitbucket Pipelines, Azure Pipelines) and maps that provider's native variable names onto teswiz's common fields (pipeline/job id + URL, short commit, who triggered it, runner); an unrecognised environment resolves as a local run (host + user). `RunEnvironment` also carries the detected provider name. `CiProvider` is the single source of truth for CI detection: `RunTelemetry.detectCiProvider()` delegates to `CiProvider.telemetryLabel(...)`, keeping its stable snake_case labels and the `RUN_IN_CI` generic-CI fallback.
- **Opt-in fail-fast (`notify.fail.on.error`, default `false`)** — notifications stay best-effort by default (a transient Slack/SMTP failure, a missing destination for an enabled channel, or a broken composer is logged and swallowed). Set `notify.fail.on.error=true` to make a run that wanted to notify but could not fail fast with a `NotificationException` naming the channel, so a misconfiguration (e.g. `notify.slack.enabled=true` with an unset or invalid webhook) is caught immediately.
- **Configurable via `NOTIFICATIONS_CONFIG_FILE`** — the notifications file is resolved from the new `NOTIFICATIONS_CONFIG_FILE` key in `config.properties` (added to the canonical template and all 54 example configs in template order), not a hardcoded classpath resource. A ready-made **disabled** `configs/teswiz/notifications.properties` ships in the repo (guarded by `SampleNotificationsPropertiesTest`), and the execution-config validator (`ConfigurationFilesConsistencyTest`) ignores `*notifications.properties` so the file can live under `configs/`.
- **Example config + docs** — added the canonical `configs/teswiz/notifications.properties.template` (lower-case `notify.*` keys, every key OFF by default), a `docs/subsystems/run-notifications.md` guide (with a Mermaid flow diagram), and wired it into the docs index, README, and architecture notes.
- All delivery-safe and unit-tested under `com.znsio.teswiz.reporting.notify` (including `CiProviderTest`, `RunEnvironmentGenericCiTest`, `BranchGateTest`, `NotifierTest`, `SlackSenderTest`, `EmailSenderTest`, and `RunNotificationsFailFastTest`).

## 🐞 Fixes

### Playwright-Java browser teardown no longer crashes Chromium on a headful run

On a headful run (e.g. macOS), the bundled Chromium ("Chrome for Testing") sometimes died with an OS "quit unexpectedly" crash dialog at the end of a run. Cause: `PlaywrightJavaWebDriver.closeSession()` closed the browser and then **immediately** stopped the Playwright node driver, orphaning the still-exiting Chromium process. Fix: a new `closeBrowserThenDriver(...)` closes the browser, **settles briefly** (`BROWSER_CLOSE_SETTLE_MILLIS`) so Chromium finishes exiting, then stops the node driver — both steps guarded and logged (at `debug`) rather than silently swallowed, so a teardown problem never propagates out of the after-scenario hook. No API change.

### Transport NSW OCR screen: route anchor fixed for reversed label

Corrected the route anchor handling in the `transportnsw_ocr` example so a reversed label resolves to the right region (`TransportNswOcrScreenWeb`, `TransportNswOcrScreenPlaywrightJava`, and the `transportnsw_ocr.feature`).

## ⬆️ Dependency Upgrades

- `google-auth-library-oauth2-http` 1.30.0 → 1.54.0
- Appium 3.8.0 → 3.9.0
- `appium-uiautomator2-driver` 8.7.0 → 8.7.2
- `appium-xcuitest-driver` 12.19.1 → 12.20.0
- Updated transitive dependencies (`@appium/base-driver`, `@appium/support`, `@appium/types`, `asyncbox`, `body-parser`, `@appium/docutils`, `lilconfig`, `yargs`) and regenerated `package-lock.json`.
- Maintains compatibility with Node.js 20.19.0+, 22.12.0+, and 24.0.0+.

## 1.0.45
# Release Notes - teswiz v1.0.45

_Previous release: v1.0.44_

## 🚀 Key Highlights & Enhancements

### 👁️ New: LLM Vision Locator (`VisionStrategy.LLM` / `VisionStrategy.OCR_THEN_LLM`)

An opt-in, vision-capable LLM can now locate an on-screen element from a natural-language description (and/or reference images) when deterministic OCR and image matching are not enough. It is reached through the same strategy-scoped entry point as OCR — `driver.vision(VisionStrategy.LLM)` or `driver.vision(VisionStrategy.OCR_THEN_LLM)` — so the calling style is unchanged; only the strategy differs.

- **Three vision strategies.** `VisionStrategy` now has `OCR`, `LLM`, and `OCR_THEN_LLM`. `OCR_THEN_LLM` tries deterministic OCR/image matching first and only falls back to the LLM on a miss (requires `IS_OCR_ENABLED=true`, since the first tier is OCR).
- **Ten providers out of the box.** `openai`, `azure-openai`, `anthropic`, `gemini`, `vertex-gemini`, `groq`, `xai`, `openrouter`, `ollama`, `bedrock`, plus `custom` for any OpenAI-compatible chat-completions endpoint. A provider factory selects the implementation from config.
- **Disabled by default and CI-safe.** The locator is off unless `LLM_VISION_FALLBACK_ENABLED=true`. The API key is read from an **environment variable** (the config stores only the env-var *name*, never the key), so it works cleanly in CI. `vertex-gemini` uses Google Application Default Credentials and `ollama` targets a local server, so neither needs an API key.
- **Fail-fast startup validation.** When enabled, teswiz validates the configuration at startup — structural checks plus, for providers that expose a non-billable models listing, a lightweight authorization health-check — so misconfiguration fails immediately rather than mid-test. The health-check target is configurable (`LLM_VISION_HEALTH_CHECK_URL`): auto-derive (default), probe an explicit URL, or skip the live probe entirely.
- **Per-call traffic logging and usage tracking.** Optional masked request/response capture is written under each scenario's `llm-vision-traffic/` report folder (`LLM_VISION_LOG_API_TRAFFIC`), and token usage is accumulated per run. Network retries and per-call timeout are configurable.

### 🔢 New: OCR read-value API (`readText` / `readValue` / `readNumber`)

The strategy-scoped finder now exposes **read-from-region** methods that return the recognised value rather than a clickable element:

- `readText(region)` / `readText(region, hint)` — return whatever text is recognised in a region.
- `readValue(region, description)` — read a described value (OCR, or LLM-assisted under the LLM arm).
- `readNumber(region)` / `readNumber(region, description)` — read and normalise a numeric value (tolerant of currency symbols, whitespace, and thousands separators), returning `Optional<BigDecimal>`.

A new `OcrReadHint` lets callers tune the read (page-segmentation mode, upscale factor, character whitelist), with digit-friendly defaults for numeric reads.

### 🖥️ HiDPI / Retina coordinate accuracy for region-scoped locates

Region-scoped visual locates now click the correct place across display environments — macOS Retina and non-Retina, external monitors, Windows, Linux, and headless.

- **One central, pure converter: `VisualRegionScaler`.** It owns every conversion between logical/CSS pixels (what the DOM and Selenium/Appium report) and physical/screenshot pixels (what a capture contains): `scaleRegionToPixels` maps a logical `VisualRegion` to the physical crop rectangle, `clampBoxToImage` keeps a located box inside the searched image (tolerating a box flush to the edge), and `toLogicalRegion` converts a located box back to absolute logical coordinates by adding the crop's physical offset **before** dividing by the runtime scale factor.
- **Scale factor is computed at runtime**, from the real screenshot width versus viewport width — never hard-coded per OS.
- **All three engines share this one conversion.** `ImageRecognitionService`, `OcrService`, and the LLM locate were each handling crop-offset/scale their own way. OCR in particular added the crop offset in physical pixels to an already-scaled logical coordinate, which mislocated region-scoped matches by the scale factor on Retina displays. They now all route through `VisualRegionScaler.toLogicalRegion`.

## 🛠️ Changed Files Summary

- `build.gradle` — version bump to `1.0.45`; dependency upgrades (Selenium `4.51.0`, Appium java-client `11.0.0`, REST-assured `6.2.0`, Applitools Eyes `5.83.13`/`5.86.13`/`5.79.55`, ONNX Runtime `1.31.0`); new `compileOnly`/`test` dependency `com.google.auth:google-auth-library-oauth2-http:1.30.0` (for `vertex-gemini` ADC); plugin bumps (`io.freefair.aspectj` `9.8.2`, `ben-manes.versions` `0.66.0`); Gradle wrapper `9.8.1`.
- `src/main/java/com/znsio/teswiz/visual/` — new LLM vision subsystem: `LlmVisionService`, `LlmVisionEngine`, `LlmVisionProvider` + `LlmVisionProviderFactory` and the ten provider classes, `LlmVisionConfigValidator`, `LlmVisionHealthCheckResolver`/`Probe`, `LlmVisionTrafficRecorder`/`CaptureWriter`, `LlmVisionUsage`/`UsageRegistry`, plus GCP token providers. New `VisualRegionScaler`; DPR/offset fixes in `ImageRecognitionService` and `OcrService`; new `OcrReadHint`.
- `src/main/java/com/znsio/teswiz/runner/` — new `VisionFinder` (strategy-scoped `findBy*` + `readText`/`readValue`/`readNumber`); `Driver`, `Visual`, `VisualFinder`, `Runner`, `Setup` extended for the vision strategies and read API.
- `src/main/java/com/znsio/teswiz/entities/VisionStrategy.java` — added `LLM` and `OCR_THEN_LLM`.
- `configs/` — added the LLM-vision property block to the canonical template and every example `*.properties` file (all commented/opt-in, defaults preserved).
- `docs/` — OCR/vision subsystem docs updated for the LLM locator, read-value API, and the `VisualRegionScaler` coordinate conversion.
- `src/test/java/com/znsio/teswiz/visual/` + `runner/` — extensive new unit coverage: per-provider tests, config-validation/health-check, traffic recorder, usage registry, JSON parsing, `VisualRegionScalerTest`, `OcrCoordinateMappingTest`, `OcrReadHintTest`, `OcrNumberNormalizationTest`, `VisionFinderReadRoutingTest`, and a `transportnsw_ocr` scenario exercising region-scoped LLM/OCR-then-LLM locates.
- `package.json` — `playwright` `1.64.1`, `@applitools/eyes-playwright` `1.51.1`, `appium-xcuitest-driver` `12.18.2`.

## ⚙️ New Configuration Properties (all opt-in, disabled by default)

Added to `configs/teswiz/teswiz_config.properties.template` (and all example configs, commented):

| Property | Purpose |
| :--- | :--- |
| `LLM_VISION_FALLBACK_ENABLED` | Master switch for the LLM vision locator (default `false`). |
| `LLM_VISION_PROVIDER` | `openai` / `azure-openai` / `anthropic` / `gemini` / `vertex-gemini` / `groq` / `xai` / `openrouter` / `ollama` / `bedrock` / `custom`. |
| `LLM_VISION_MODEL` | Model id. Optional for openai/azure/anthropic/gemini/vertex-gemini; mandatory for groq, xai, openrouter, ollama, bedrock. |
| `LLM_VISION_ENDPOINT` | Vision chat-completions endpoint. |
| `LLM_VISION_API_KEY_ENV` | Name of the env var holding the API key (never the key itself). |
| `LLM_VISION_API_VERSION` | Azure OpenAI api-version (default `2024-06-01`). |
| `LLM_VISION_HEALTH_CHECK_URL` | Startup health-check target: blank (auto-derive), explicit URL, or `none`/`off`/`false` to skip. |
| `LLM_VISION_MAX_RETRIES` | Network retries per LLM call (default `1`). |
| `LLM_VISION_TIMEOUT_SECONDS` | Per-call network timeout (default `30`). |
| `LLM_VISION_LOG_API_TRAFFIC` | Write masked per-call request/response files (default `true`). |
| `LLM_VISION_SAVE_CAPTURES` | Save the images sent to the LLM for diagnostics (default `true`). |

## ⬆️ Dependency & Tooling Updates

- Selenium `4.50.0` → `4.51.0`
- Appium java-client `10.1.1` → `11.0.0`
- REST-assured `6.0.1` → `6.2.0`
- Applitools Eyes: images `5.83.9` → `5.83.13`, playwright `5.86.9` → `5.86.13`, appium `5.79.50` → `5.79.55`
- ONNX Runtime `1.30.0` → `1.31.0`; Byte Buddy `1.18.14` → `1.18.15`
- New: `com.google.auth:google-auth-library-oauth2-http:1.30.0` (for `vertex-gemini` ADC)
- Gradle wrapper `9.8.0` → `9.8.1`; AspectJ weaving plugin `9.8.0` → `9.8.2`; versions plugin `0.65.0` → `0.66.0`
- Node: `playwright` `1.63.0` → `1.64.1`, `@applitools/eyes-playwright` `^1.49.3` → `1.51.1`, `appium-xcuitest-driver` `12.15.1` → `12.18.2`

## 📝 Notes & Compatibility

- **No breaking API changes.** The LLM locator and read-value methods are additive; existing OCR/image usage is unaffected. `VisionStrategy.OCR` behaviour is unchanged.
- **LLM vision is strictly opt-in.** It stays disabled unless `LLM_VISION_FALLBACK_ENABLED=true`, and `OCR_THEN_LLM` additionally requires `IS_OCR_ENABLED=true`.
- **Playwright browser binaries:** this release bumps Playwright to `1.64.1`. After upgrading, run `./node_modules/.bin/playwright install` (or `npx playwright install`) so the matching browser binaries are present, otherwise Playwright-engine tests fail to launch a browser.

## 1.0.44
# Release Notes - teswiz v1.0.44

_Previous release: v1.0.43_

## 🚀 Key Highlights & Enhancements

### 🧭 Engine-native Playwright locators — Selenium `By` bridge removed from Playwright

- **Playwright is now authored natively; the Playwright↔Selenium-`By` translation bridge is deleted.** pw-java screens use `context.page().locator(...)` / `getByText(...)` from `PlaywrightJavaScreenContext`; pw-ts screens use their `.screen.ts` modules. `By` is now the locator currency for **Selenium and Appium only**.
- **Selenium and Appium are completely unchanged** — zero behaviour or public-API change. The `By` coupling lived only in the `web.playwright` package; `ElementWaiter`, the `Driver` `By`-methods, and `ElementFinder` call the Selenium `WebDriver` interface and are engine-neutral.
- **`findElement(By)` / `findElements(By)` and `By`-based waits now fail fast on the Playwright engines** with a structured `WebEngineCapabilities` diagnostic (new `WebCapability.ELEMENT_LOCATION_BY_SELENIUM_LOCATOR`) that points authors at native Playwright authoring, instead of silently translating.
- **Removed** (Playwright-only, no external users): `PlaywrightBy`, `PlaywrightJavaBy`, `PlaywrightLocator`, `PlaywrightLocatorReference`, the pw-ts `PlaywrightWebElement` + `PlaywrightShadowRootSearchContext`, and the `worker.mjs` locator protocol (`createLocator` / `elementAction` / `countElements`). The native `screenAction` worker path is retained. `PlaywrightJavaWebElement` is retained as the native `Locator` adapter.
- **New native bounded waits** on `PlaywrightJavaWait` (`untilVisible` / `untilPresent` / `untilHidden`) so Playwright loses no wait capability when the `By`-waits stop routing through it.

### 🧭 Cross-engine bounded element waits (`ElementWaiter`)

- **New `ElementWaiter` in `com.znsio.teswiz.runner`**, surfaced through three delegating methods on the `Driver` facade: `isElementVisible(By, int)`, `isElementPresentWithin(By, int)`, and `waitTillTextIsPresent(By, String, int)`. Each waits up to a bounded number of seconds and **returns `false` on timeout instead of throwing** — the "is it there within N seconds?" boolean that the existing `waitTill*` methods lacked (they all throw on timeout). Additive and non-breaking: no existing signature changed. On Selenium/Appium, `By` is the locator currency for these waits; Playwright screens use native waits (`PlaywrightJavaWait`).

### 🔌 Web-engine capability parity signalling

- **`WebCapability` gained `SHADOW_DOM` and `ALERTS`.** On `playwright-java`, `getShadowRoot()` and `switchTo().alert()` now throw a structured `WebEngineCapabilities` diagnostic naming the capability and the Playwright-native alternative, instead of a bare/absent failure — the same pattern already used for windows/positioning.

### 📐 Consistent default window/viewport sizing (`WindowSizingResolver`)

- **New `WindowSizingResolver` is the single source of truth for the default viewport.** Selenium's hard-coded `1920x1080` headless fallback and the Playwright context-options viewport default now both resolve from it (`TESWIZ_DRIVER_VIEWPORT_WIDTH` / `HEIGHT`, default `1280x960`), so a sizing override applies consistently across engines. No config schema change.

### 🖍️ Universal element highlighting (all engines)

- **Element highlighting is now universal, not just for OCR / `WebElement` actions.** Previously a Screen driving the raw Playwright API (`page().locator(sel).click()`) got no highlight, because the highlight lived only inside teswiz's `WebElement` adapter. New transparent `HighlightingPage` / `HighlightingLocator` / `HighlightingFrameLocator` (JDK dynamic proxies over Playwright's `Page` / `Locator` / `FrameLocator` interfaces) are installed at the single `Page`-creation point, so every action (`click`, `fill`, `press`, `check`, `selectOption`, …) highlights the element first — with **no change to the calling test and no `org.openqa.selenium` types**. Chained / `frameLocator` results are re-wrapped so highlighting survives locator chaining; unknown future Playwright methods forward verbatim.
- **The Playwright highlight JS is extracted into one `PlaywrightHighlighter`** reused by both the `WebElement` adapter and the new proxies (was duplicated). Gated on `HIGHLIGHT_ELEMENTS`; never throws (a highlight must never block the action it precedes).
- **Closed two Selenium highlight bypasses:** `Driver.allowPermission` and `Driver.uploadFileInBrowser` now resolve elements through the decorating `findElement(...)` facade, so their `click` / `sendKeys` highlight like every other Selenium action. pw-ts and Appium already highlighted via their `WebElement` / `VisualElement` paths.

## 🧪 Tests

- Added `ElementWaiterTest` (6), `WindowSizingResolverTest` (3), and `PlaywrightJavaWaitTest` (6, native Playwright waits).
- Added `PlaywrightHighlighterTest` and `HighlightingLocatorTest` for universal element highlighting.
- `PlaywrightLocatorTest` removed with the `By` bridge.
- `PlaywrightWebDriverTest` reworked to assert the native pw-ts contract (surviving native driver ops + unsupported-signal guards for the removed `By` surface).
- `GeneratedScreenParityTest` pw-java leg now runs a real native Playwright Java driver (`PlaywrightJavaWebDriverFixture`).
- Full `clean test` green: **634 tests, 0 failures, 15 skipped**.

## ⬆️ Upgrade notes

- **No action needed for Selenium or Appium users** — behaviour and public API are unchanged.
- **Playwright screen authors:** if any pw-java/pw-ts screen still called `driver.findElement(By...)` or a `By`-based wait, migrate it to native authoring (pw-java: `context.page().locator(...)` / `getByText(...)`; pw-ts: `.screen.ts`). Those calls now throw a structured capability error on Playwright engines.

## 1.0.43
# Release Notes - teswiz v1.0.43

_Previous release: v1.0.42_

## 🚀 Key Highlights & Enhancements

### 👁️ Visual Subsystem — wait-until-visible and screen-layer consolidation

* **New `waitUntilVisualElementIsVisible*` methods on `VisualOcrBL`.** Four locator variants — by OCR text, by image template, by fallback text-or-image, and by fallback image-or-text — each with a default-budget overload and an overload taking an explicit maximum wait in seconds. They poll the existing engine-agnostic finders until the element is located on screen (non-null = visible) or the budget elapses, then assert visibility. The default budget derives from `VISUAL_ELEMENT_RETRY_ATTEMPTS × VISUAL_ELEMENT_RETRY_DELAY_SECONDS`; the inter-poll sleep is `VISUAL_ELEMENT_RETRY_DELAY_SECONDS`. Paired Gherkin steps (default and `within {int} seconds`) are added for all four variants. Works for Selenium, `playwright-java`, `playwright-ts` and Appium with no new TypeScript worker primitive, because the polling lives in the business layer and drives the already-abstract find operations.
* **The four `VisualOcrScreen` platform classes are consolidated behind a shared template.** `VisualOcrScreen` is now a pure abstract contract; a new `AbstractVisualOcrScreen` owns the single find → assert → highlight → screenshot(before) → act → wait → screenshot(after) choreography plus `parsePositionIndex`. The Android/iOS/web/playwright-java classes reduce to a constructor (Android/iOS additionally override an `afterTextEntry()` hook to dismiss the keyboard), removing roughly 1,400 lines of duplication. The contract stays abstract with a no-arg constructor so the dynamically generated `playwright-ts` bridge continues to work unchanged.

### 🧱 Visual Subsystem — encapsulation and SOLID clean-up

* **Gesture dispatch moved out of `VisualElement` into the `Driver` facade.** `VisualElement` is now a pure immutable value object (bounds + label) that forwards each gesture to the facade with its own centre/geometry; the engine-specific "how" lives in a new `VisualGestureDispatcher` (Appium touch → native coordinate input → Selenium `Actions` → synthesised DOM events). This removes the driver-type `instanceof` ladders and the `getInnerDriver()`/`getType()` coupling from the element.
* **`VisualElement`'s `WebElement` adapter extracted** into `VisualElementWebElementAdapter`, so the value object no longer also carries the Selenium-proxy responsibility.
* **`VisualOcrScreen` contract segregated** into role interfaces `VisualElementFinder`, `VisualElementActions`, and `VisualElementInspector`; the class remains the aggregate so callers and the `playwright-ts` bridge are unaffected.
* **Unsupported gestures now throw `UnsupportedVisualGestureException`** instead of logging and continuing. A swipe/long-press on a plain Selenium web driver (no touch, no native coordinate input) and `zoom`/`pinch` on any engine fail loudly rather than passing against an unchanged app. **Parity note:** `click`/`doubleClick`/`hover`/`sendKeys` work on all engines; `swipe`/`longPress` require touch (Appium) or native coordinate input (Playwright), so parity is intentionally not provided on a mouse-only Selenium web driver.

### 🧹 Logging Modernization

* **Migrated the last four Log4j 1.x files to Log4j2** (`SessionContext`, `TestExecutionContext`, `CucumberScenarioListener`, `CucumberScenarioReporterListener`) and removed the `log4j1.compatibility` shim. No source now imports `org.apache.log4j.*`.

### 👁️ Visual Subsystem — self-clearing gestures and diagnostic captures

* **Visual gestures now clean up their own highlight overlay.** `VisualElement.click()`, `doubleClick()`, `hover()`, `longPress()`, `sendKeys()`, `swipe()` and `dragAndDropTo()` now clear the highlight they draw, via a `try/finally` around the gesture body (each body extracted into a `performX()` helper). Previously the highlight was removed only by the `VisualOcrScreen.clickVisualElementBy*` ritual, so any caller that located an element and acted on it directly (e.g. `findVisualElementByImage(...).click()`) left the orange highlight box painted on the page. On a canvas with the SPIN control at the same position across screens, that leftover overlay was then captured in the next locate screenshot and masked the template, breaking the subsequent match. The cleanup is driver-agnostic — `Driver.clearHighlight()` already no-ops safely on Appium — so Selenium, `playwright-java`, `playwright-ts` and Appium all benefit.
* **Before/After visual captures are now plain diagnostic screenshots, not Applitools baseline checks.** Across all four `VisualOcrScreen` implementations (web, playwright-java, iOS, Android) the per-action `visually.checkWindow(...)` Before/After calls are replaced with `visually.takeScreenshot(...)`, and the now-redundant explicit `driver.clearHighlight()` lines are removed (the gesture self-clears). These captures were always meant as "what did the action do" evidence, not product visual-regression assertions; routing them through `takeScreenshot` keeps the framework-injected highlight out of consumers' Applitools baselines and stops incidental per-action pixel changes raising false visual diffs.
* **Breaking change for visual-regression users:** the ~60 per-action `checkWindow` checkpoints across the four Screen classes are gone. Consumers who relied on those as regression gates will no longer get an Applitools comparison on each visual action; add explicit `checkWindow` calls where a product-UI baseline is genuinely wanted.

---

## 🛠️ Changed Files Summary

* `build.gradle` — Version bump to `1.0.43`.
* `src/main/java/com/znsio/teswiz/businessLayer/visualocr/VisualOcrBL.java` — Added `waitUntilVisualElementIsVisible*` methods (four variants, each with default-budget and explicit-timeout overloads).
* `src/test/java/com/znsio/teswiz/steps/VisualOcrSteps.java` — Added paired Gherkin step bindings (default and `within {int} seconds`) for the new wait methods.
* `src/main/java/com/znsio/teswiz/screen/.../VisualOcrScreen*.java` — Consolidated the four platform classes behind a new `AbstractVisualOcrScreen` template; `VisualOcrScreen` reduced to a pure abstract contract.
* `src/main/java/com/znsio/teswiz/visual/VisualGestureDispatcher.java` — New engine-specific gesture dispatcher (Appium touch → native coordinate input → Selenium `Actions` → synthesised DOM events).
* `src/main/java/com/znsio/teswiz/visual/NativeCoordinateInput.java` — New native coordinate input used for Playwright gestures.
* `VisualElement` / `VisualElementWebElementAdapter` — `VisualElement` reduced to an immutable value object; `WebElement` adapter extracted into its own class.
* Role interfaces `VisualElementFinder`, `VisualElementActions`, `VisualElementInspector` — Segregated contract for the visual screen.
* Log4j2 migration — `SessionContext`, `TestExecutionContext`, `CucumberScenarioListener`, `CucumberScenarioReporterListener`; removed the Log4j 1.x compatibility shim.
* Tests — `VisualElementForwardingTest`, `VisualElementHighlightTest`, `VisualGestureDispatcherTest` added.

## 1.0.42
# Release Notes - teswiz v1.0.42

_Previous release: v1.0.41_

## 🚀 Key Highlights & Enhancements

### 🌐 Playwright Engine (`playwright-java` & `playwright-ts`)
* **Maximized Window Mode Parity**: Fixed viewport resolution in `PlaywrightBrowserConfigResolver` to omit fixed viewport size (`1280x960`) when `"maximize": true` or `--start-maximized` is configured. This allows Playwright Java and TS engines to honor full-screen maximized windows without fixed viewport emulation overriding `--start-maximized`.
* **Dynamic Window Controls**: Updated `PlaywrightJavaWebDriver.manage().window().maximize()` to expand viewport to screen available size (`window.screen.availWidth` x `availHeight`), and `getSize()` to evaluate actual inner window dimensions when viewport is `null`.

### 👁️ Visual Subsystem & OCR Logging
* **OCR Region Log Formatting**: Improved logger output in `Visual.java` to display `[entire screen]` instead of raw Java `null` when performing OCR or image template detection without an explicit bounding box crop.

### 🧹 Configuration & Repository Cleanups
* **Configuration Normalization**: Removed redundant `# Historical properties...` and `# Project/provider-specific properties...` comment lines across all `configs/**/*.properties` files.
* **Gradle Plugin Modernization**: Updated deprecated `com.github.ben-manes.versions` to `io.github.ben-manes.versions` in `build.gradle`.

---

## 🛠️ Changed Files Summary

* `build.gradle` — Version bump to `1.0.42` and plugin ID update.
* `src/main/java/com/znsio/teswiz/config/browser/PlaywrightBrowserConfigResolver.java` — Maximized viewport resolution logic.
* `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightJavaDriverManager.java` — Handled `null` viewport size in context options.
* `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightJavaWebDriver.java` — `maximize()` and `getSize()`.
* `src/main/java/com/znsio/teswiz/runner/Visual.java` — Formatted `null` OCR region logging output as `[entire screen]`.
* `src/test/java/com/znsio/teswiz/config/browser/PlaywrightBrowserConfigResolverTest.java` — Maximized viewport cases.
* `configs/**/*.properties` — Cleaned up redundant comment headers.
* `package.json`, `package-lock.json`, `docs/getting-started/getting-started-guide.md` — Version bump to `1.0.42`.

---

## 📋 Installation & Upgrade

To use version `1.0.42` in your Gradle project:

```groovy
dependencies {
    implementation 'com.github.anandbagmar:teswiz:1.0.42'
}
```

## 1.0.41
# Release Notes - teswiz v1.0.41

## 🚀 Key Highlights & Improvements

### 🌐 Playwright Engine (`playwright-java` & `playwright-ts`)
* **Default Browser Launch Alignment**: Aligned `playwright-java` browser launching with standard Playwright conventions, defaulting to Playwright’s bundled Chromium across local and CI environments without unnecessary channel overrides.
* **Navigation & Timeout Resiliency**: Implemented graceful fallback to `COMMIT` navigation in `PlaywrightJavaWebDriver` and Node worker script (`worker.mjs`) when external third-party assets (such as tracking/analytics scripts) cause `DOMCONTENTLOADED` timeouts.
* **Synchronized Page Transitions**: Updated screen element handlers to wrap link clicks with `page.waitForNavigation()` for deterministic page state transitions.
* **Visual Testing Stability**: Disabled CSS animations during screenshot capture and aligned navigation wait strategies to eliminate visual check flakes.

### 📡 API Subsystem & Telemetry
* **Unified API Engine Access**: Refactored `ApiService` and `RunTelemetry` to utilize the new `Runner.getApiEngine()` helper for consistent engine selection (`rest-assured`, `playwright-java`, `playwright-ts`).
* **External API Compliance**: Updated API Business Layer requests to include standard `User-Agent` headers, resolving HTTP `403 Forbidden` policy blocks on third-party services like OpenStreetMap/Nominatim.

### 📦 Dependencies & Compatibility
* **ReportPortal Cucumber 8 Support**: Upgraded `agent-java-cucumber7` to `5.4.10` for seamless compatibility with Cucumber 8 and TestNG reporting.
* **Linux CI Tesseract Compatibility**: Pinned `tess4j` to `5.11.0` to resolve native `libtesseract` symbol loading errors (`pixBackgroundNormTo1MinMax`) on Linux runner environments.
* **Deprecation Cleanups**: Removed deprecated `AbstractTestNGCucumberTests` inheritance from Cucumber glue runners (`RunCukes` and `RunTestCukes`).
* **Configuration Cleanup**: Removed unused legacy configuration properties and streamlined property template contracts.

---

## 🛠️ Summary of Changed Files

* `build.gradle` — Version bump to `1.0.41` and ReportPortal agent upgrade.
* `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightJavaWebDriver.java` — Navigation fallback to `COMMIT` on timeout.
* `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightJavaDriverManager.java` — Aligned browser channel defaults.
* `playwright/worker.mjs` — Added navigation fallback handling for Node Playwright runner.
* `src/main/java/com/znsio/teswiz/services/ApiService.java` & `src/main/java/com/znsio/teswiz/runner/Runner.java` — Added `Runner.getApiEngine()` helper and refactored client resolution.
* `src/main/java/com/znsio/teswiz/listener/CucumberScenarioReporterListener.java` — ReportPortal listener updates.
* `src/main/java/com/znsio/teswiz/steps/RunCukes.java` & `src/test/java/com/znsio/teswiz/steps/RunTestCukes.java` — Removed deprecated Cucumber TestNG class extensions.
* `src/test/java/com/znsio/teswiz/businessLayer/weatherAPI/WeatherAPIBL.java` — Added `User-Agent` header for Nominatim API endpoints.
* `src/test/java/com/znsio/teswiz/screen/web/playwrightjava/theapp/AppLaunchScreenPlaywrightJava.java` — Synchronized page navigation.

---

## 📋 Upgrade Instructions

Update `build.gradle` in your project:

```groovy
dependencies {
    implementation 'com.github.anandbagmar:teswiz:1.0.41'
}
```

## 1.0.40
- Update publishRelease.sh, AGENTS.md, and Changelog
- Add documentation links across Capability Matrix and README
- Refactor API client, retry logic, and test metadata
- chore: update dependencies to latest versions
- Update dependencies: bump appium to 3.8.0 and webdrivermanager to 6.4.0
- Enhance formatting and visual layout across all docs
- Fix case-sensitive links in documentation
- Enhance documentation visual layout and formatting
- Rework Key Architectural & Upgrade Notes in README
- Rename docs/README.md to docs/index.md
- Reorganize documentation directory layout
- Update architecture diagrams across documentation
- Add main resources path to Playwright worker
- Move Playwright TS screens to main resources
- Add Transport NSW capabilities JSON & enable CAPS property
- Enhance Visual OCR support for Playwright engines
- Make visual retry configurable & update code quality rules
- Add region visual search, retry & highlight configs
- Fix anchor resolution fallback in visual proximity
- Feat: add findAll, proximity, and position locators
- Feat: add findAll, spatial relative, and VisualBy proxy locators
- Add 2-pass multi-angle text OCR search in OcrService
- Add multi-angle rotation scanning fallback to OcrService
- Clear visual highlights post-action and standardize VisualOcrBL return types
- Eliminate redundant OCR searches and locator screenshots
- Add verification and conditional action methods to VisualOcrBL
- Refactor OcrService and VisualOcrSteps to BL architecture
- Improve OCR matching accuracy and log fallback option
- Disable element highlighting in GitHub Actions workflows
- Add scenarios for highway navigation and include motorway image template
- Add element highlighting for standard web element interactions
- Add HIGHLIGHT_ELEMENTS config and update visual docs
- Add OCR line matching and visual screenshot retries
- Load IS_OCR_ENABLED and threshold in Setup properties
- Add environment base URLs for COVID tracking and Transport NSW
- Add positive Visual OCR BDD test suites and update rules
- Refactor Visual OCR test suite to follow Feature-Step-BL-Screen pattern
- Add embedded Visual OCR & Image Recognition subsystem
- docs: update OCR and Image Recognition implementation roadmap with status and deliverables
- Add OCR and visual locator facade to Driver
- refactor: remove Playwright API test support ideas from backlog
- fix: catch EyesException in instantiateAppiumEyes

## 1.0.39
- docs: add RestAssured to Playwright API migration
- fix: set Connection close for Playwright API
- fix: set User-Agent and timeout for Playwright API
- fix: resolve crypto 451 and Playwright GET timeout
- fix: restore @api tag on negative tests and exclude
- feat: support playwright-ts & align commit rules
- fix: isolate mock failing test suites from @api
- fix: resolve API failures & payload serialization
- ci: log test command & config overrides in GHA
- feat: expand PlaywrightBy with native Playwright locator strategies
- feat: add web_api_hybrid_workflow & update engine docs
- ci: add API_Engines_CI workflow & consolidate web scenarios
- refactor: consolidate API parity test into multi-tagged scenario
- docs: add pw-ts Cucumber scenario and CLI examples
- ci: add API workflow chaining scenarios & CI execution
- feat: enhance ApiService feature parity & migrate BLs
- feat: add multi-engine Cucumber tests and architecture docs
- feat: add PUT, HEAD, OPTIONS and HTML support to ApiService
- feat: add Playwright API testing engine capability

## 1.0.38
- Add web engine capability guide and update README links
- Refactor parity steps to BL and Screen contracts
- Add Cucumber BDD scenarios for web engine parity
- Implement web engine parity and capability registry
- feat: add Playwright API testing engine implementation and migration plan

## 1.0.37
- updated package-lock.json
- chore: update Gradle wrapper to version 9.7.1
- chore: update Gradle wrapper to version 9.7.1
- chore: update plugin and dependency versions in build.gradle
- feat: add dependabot configuration for npm and Gradle updates
- chore: update dependencies to latest versions
- Update playwright to version 1.62.1 in package.json and package-lock.json
- Add step attribution for soft assertion failures

## 1.0.36
- Include the executed command in commandOutput artifact files
- Use version as default release title

## 1.0.35
- Handle existing release tags safely
- Make multi-line release paste explicit
- Support multi-line release descriptions
- Add opt-in per-scenario API traffic logging; make EnvironmentIssueFilter global
- Fix Playwright TS file upload selector
- Fix Playwright Java file upload wait
- Run config consistency checks across Gradle tasks
- Enforce execution config template consistency
- Normalize all execution properties to canonical template
- Enforce consistency across execution configurations
- Add inline configuration example
- Make configuration template the documentation source of truth
- Add complete Teswiz execution configuration template Consolidate Teswiz configuration templates
- Add canonical runtime configuration template
- Centralize driver wait and retry defaults Add validated runtime defaults configuration
- Make app download tests deterministic Set configurable app timeout default to 15 seconds
- Bound app artifact HTTP requests with timeouts Make app download timeout configurable
- Reduce noisy test logging and fix rollover granularity
- Add logging regression tests
- Improve logging, diagnostics, and execution summaries
- Add interactive release content review loop with revise and abort cleanup
- Improve console and test logging readability
- Add progress-enabled GitHub asset uploads for release jars
- Improve release note generation and filter README update commits

## 1.0.34
- Handle already-set npm version during release reruns
- Enforce main-branch release and rebase before build
- Release 1.0.34
- 🔄 Update README with latest commit ID - e02f3ad
- fix(masking): mask compound query-string keys without swallowing adjacent params
- 🔄 Update README with latest commit ID - cd6a7f3
- 🔄 Update README with latest commit ID - a72d92d
- docs(readme): document new TestNG-only FRAMEWORK execution mode
- 🔄 Update README with latest commit ID - d6dcdda
- feat(masking): allow configuring additional/override sensitive log keys
- Add TestNG-only execution mode as an alternative to Cucumber-JVM (#4)
- 🔄 Update README with latest commit ID - 15ab312

## 1.0.33
- Fix duplicated URL in EnvironmentIssueFilter error message
- 🔄 Update README with latest commit ID - 47400ac

## 1.0.32
- Add tests for EnvironmentIssueFilter and its opt-out flag
- 🔄 Update README with latest commit ID - 0c574e4
- Wire EnvironmentIssueFilter into RestAssuredService, on by default
- 🔄 Update README with latest commit ID - a83cb28
- Track docs/images/ despite the blanket *.png gitignore rule
- Fix stale org references and broken dependency guidance across docs
- Add DocumentationLinksTest to guard against broken doc links
- Fix broken links across README and docs
- 🔄 Update README with latest commit ID - 3c7981e
- Refactor publishRelease.sh into small, named functions
- Retry Jitpack build trigger once if the tag isn't visible yet
- Trigger Jitpack build right after tag push, before the slow fat jar upload
- 🔄 Update README with latest commit ID - 5fa27c1

## 1.0.31
- Correct api/implementation split: reflection/weaving isn't a real API leak
- 🔄 Update README with latest commit ID - c25dadc

## 1.0.30
- 🔄 Update README with latest commit ID - 4b96e86
- Fix flaky BrowserStack executor command test after RestAssured migration
- Publish teswiz as both a thin jar and a shaded "all" fat jar
- Stop building the 486MB shadow jar as part of assemble/build
- Migrate HTTP client from Unirest to RestAssured
- 🔄 Update README with latest commit ID - f8c27a6

## 1.0.29
- 🔄 Update README with latest commit ID - 238ffe7
- Fix screen resolver to support consumer-defined package namespaces Update Playwright docs to reflect configurable screen-contract package convention
- 🔄 Update README with latest commit ID - c5cf675

## 1.0.28
- 🔄 Update README with latest commit ID - c07f8f3
- Align Playwright version and fix BrowserStack capabilities
- 🔄 Update README with latest commit ID - 011650c
- Use layout algo to compare the "Vertical Swiping Screen After Scroll" screen
- Gracefully handle WebDriverException during teardown
- BrowserStack requires both browserstack.playwrightVersion and client.playwrightVersion to match the local client's Playwright library version exactly
- Align BrowserStack Playwright capabilities version and add diagnostics
- Improve release badge styling and add worker diagnostics
- 🔄 Update README with latest commit ID - ab5305a
- Fix Playwright TS worker page.goto timeout error
- 🔄 Update README with latest commit ID - 7788d4c
- Fix release version script and sync package-lock.json
- Sync package-lock.json with package.json
- Migrate Playwright-TS screens to native selectors
- Migrate GoogleSearch and Indigo Playwright-Java screens to native APIs
- Migrate JioMeet Playwright-Java screens to native APIs
- feat: add native playwright-java screen context and convert theapp screens
- fix: use plain version tags in publishRelease

## 1.0.27
- refactor: prompt for unit-test execution in publishRelease
- docs: remove outdated repository notice from README
- feat: add skip-unit-tests flag to publishRelease script
- Update CI workflows to restrict push events to the main branch only
- 🔄 Update README with latest commit ID - 83d51a9
- Add first-class Playwright Java and Playwright TS web engines to teswiz (#2)
- 🔄 Update README with latest commit ID - 407d445
- Update environment variable for Applitools API key in build.gradle
- 🔄 Update README with latest commit ID - a237f60
- Create codeql.yml
- 🔄 Update README with latest commit ID - db402e1
- Fix GitHub Action permission issues
- Add publishRelease.sh automation script
- Point GitHub workflows, badges, and build configs to anandbagmar fork
- Add temporarily moved notice to README.md
- fix(deps): patch @appium/support, ajv, @babel/core, diff transitive vulnerabilities
- Remove JioMeet-Lite binary path from browser configuration for Electron apps
- fix(deps): reduce vulnerability backlog in teswiz
- fix(deps): reduce high and critical npm vulnerabilities in teswiz

## 1.0.26

### Driver enhancements

- Added a reusable page source dump helper that writes `dom-dump-<index>.html` for web and `hierarchy-dump-<index>.xml` for Android/iOS into the active scenario log directory.
- Added per-scenario dump indexing so repeated calls create sequential files that reset with each scenario.
- Reorganized driver import statements for clarity.

### Tests

- Updated the TheApp invalid-login scenario to execute the second login attempt again so the web flow covers the reusable invalid-login path.

## 1.0.25

### Runner

- Auto-include the `com.znsio.teswiz.steps` package as a second `--glue` entry so reusable built-in steps (`FigmaSteps`, `CommonSteps`, etc.) are discoverable in consuming projects without extra glue configuration.

### Driver enhancements

- Added a maximum scroll attempts limit and improved element visibility checks.

### Figma and Applitools

- Added the missing step to navigate to the application page in the Figma design scenario.
- Updated UFG device emulation to iPhone 15 Pro in the UFG configuration.
- Removed unused driver imports and driver creation call in the Figma step.

## 1.0.24

### Driver enhancements

- Enhanced click methods and context switching functionality in the driver.

### Build and dependency updates

- Updated `appium-xcuitest-driver` to version `11.13.0`.
- Updated Gradle to `9.6.0`.
- Updated all Java dependencies.
- Updated GitHub Actions to v7.
- Refreshed npm/node packages.

## 1.0.23

### Build and dependency updates

- Changed dependency download behavior so `forceUpdate` now defaults to `false`.
- Added a Gradle override so `-PforceUpdate=true` forces a fresh jar download when needed.
- Updated the README guidance to document the Gradle property-based override.
- Kept `build.gradle.sample` aligned with the new default and override flow.

## 1.0.22

### Build and dependency updates

- Updated the Gradle wrapper to `9.5.1`.
- Improved Gradle launcher script compatibility in `gradlew` and `gradlew.bat`.
- Refreshed npm dependencies in `package.json` and regenerated `package-lock.json`.
- Updated Gradle plugin and dependency versions in `build.gradle`.

## 1.0.21

### Visual testing

- Fixed Applitools mobile capability setup for Appium visual runs so it uses the initialized Applitools configuration map correctly.
- Scoped Applitools mobile capability injection to runs where both `IS_VISUAL=true` and `useNML=true`.
- Simplified proxy settings initialization in `Visual.addMobileCapabilitiesToTestContextForVisualTesting`.

## 1.0.20

### Visual testing

- Added support for Native Mobile Layout configuration through `useNML`.
- Added platform-specific NML device configuration support from test context for Android and iOS visual runs.
- Updated visual setup so NML supports one or many configured mobile device targets.

### Figma and Applitools baseline comparison

- Replaced the old scenario-tag-based `@eyes-...` baseline flow with an explicit Cucumber step:
  `I have my Figma design with app name "...", test name "..." and baseline name "..." available in Applitools`
- Added new `TestExecutionContext` keys for:
  - `APPLITOOLS_FIGMA_APP_NAME`
  - `APPLITOOLS_FIGMA_TEST_NAME`
  - `APPLITOOLS_FIGMA_BASELINE_ENV_NAME`
- Updated `Visual` to use the explicit Figma-provided app name, test name, and baseline environment name before creating Applitools sessions.
- Added validation so partial or blank Figma/Applitools naming input fails fast with `VisualTestSetupException`.

### Tests

- Added focused unit tests for Figma/Applitools context resolution in `VisualTest`.
- Added focused unit tests for storing Figma naming details in `FigmaStepsTest`.

### Documentation

- Updated `README.md` and `docs/features/RunningVisualTests-README.md` to document:
  - `useNML`
  - the explicit Figma/Applitools naming step
  - the sample `applitools.feature` usage
- Removed stale documentation references to the old `@eyes-...` approach.
