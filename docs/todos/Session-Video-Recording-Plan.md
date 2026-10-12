# teswiz Session Video Recording — Plan & TODO

> Status: **Proposed — awaiting clarifications before implementation.**
> Capture a video recording of every UI session (browser or device) when running
> any UI test, across all engines and in both head and headless mode.
>
> Scope engines: Selenium, Playwright-Java, Playwright-TS, Appium (Android & iOS).

## Goal

When running any UI test — using Selenium, Playwright-Java, Playwright-TS, or Appium
(Android/iOS), in head mode or headless — capture a video recording of the
browser/device session and attach it to the scenario report.

## Current state (verified in codebase)

- **No video/screen recording exists anywhere.** No Appium `startRecordingScreen` /
  `stopRecordingScreen`, no Playwright `recordVideo`, no CDP screencast.
- Still-image screenshots do exist: `src/main/java/com/znsio/teswiz/tools/ScreenShotManager.java`
  (`takeScreenShot(innerDriver, tagName)`) writes a PNG and attaches it to ReportPortal.
- Driver creation forks per platform in
  `src/main/java/com/znsio/teswiz/runner/Drivers.java` → `createDriverForPlatform(...)`,
  and for web again in
  `src/main/java/com/znsio/teswiz/web/browser/BrowserDriverManager.java` →
  `createWebSessionForUser(...)` across `SELENIUM` / `PLAYWRIGHT_JAVA` / `PLAYWRIGHT_TS`.
- Teardown flows through `Drivers.attachLogsAndCloseAllDrivers(...)` →
  `attachLogsAndCloseDriver(...)` → engine-specific `closeWebDriver` / `closeAppiumDriver`.
- Playwright-Java context options are built in
  `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightJavaDriverManager.java`
  `buildContextOptions(...)` (already sets `setRecordHarPath`, starts tracing).
- Playwright-TS context is built in `playwright/worker.mjs` `newContext(...)` (~line 599,
  already sets `recordHar` and starts tracing).
- Artifacts are written under `target/reports/<scenario>/` (see
  `src/main/java/com/znsio/teswiz/runner/FileLocations.java` and `CucumberScenarioListener`)
  and auto-published by
  `src/main/java/com/znsio/teswiz/reporting/ScenarioArtifactReporter.java`, which has an
  explicit suffix/metadata registry for Playwright artifacts
  (`PLAYWRIGHT_ARTIFACT_SUFFIXES` / `PLAYWRIGHT_ARTIFACT_METADATA_KEYS`).

## Mechanism per engine (no single API covers all four)

| Engine | Mechanism | Headless | Notes |
|---|---|---|---|
| Playwright-Java | `Browser.NewContextOptions.setRecordVideoDir(...)` (+ optional `setRecordVideoSize`) in `buildContextOptions(...)` | Yes | Video finalizes on `context.close()`; filename is Playwright-generated — capture `page.video().path()` at teardown |
| Playwright-TS | `contextOptions.recordVideo = { dir }` in `worker.mjs` `newContext(...)`; return path over the worker protocol | Yes | Mirror existing `recordHar`/trace plumbing in `PlaywrightWorkerManager` metadata |
| Appium Android | `startRecordingScreen` / `stopRecordingScreen` (base64 mp4) in `AppiumDriverManager` | N/A (device) | Cloud providers may offer their own video; emulators can be flaky |
| Appium iOS | same Appium commands | N/A (device) | Simulator recording needs host dependencies (ffmpeg/mjpeg) |
| Selenium | No native video. Options: provider-side video (BrowserStack/grid), CDP `Page.startScreencast` frame-stitch (Chromium only), or no local support | CDP screencast works headless | Weakest / most expensive path |

## Shared work (all engines)

1. New config property `CAPTURE_SESSION_VIDEO` (default `false`):
   - Add to `configs/teswiz/teswiz_config.properties.template` **first** (canonical contract).
   - Add (active or commented with default) to every `configs/**/*.properties`.
   - Consume in `src/main/java/com/znsio/teswiz/runner/Setup.java` using the standard
     boolean pattern:
     `configsBoolean.put(CAPTURE_SESSION_VIDEO, getOverriddenBooleanValue(CAPTURE_SESSION_VIDEO, getBooleanValueFromPropertiesIfAvailable(CAPTURE_SESSION_VIDEO, false)));`
   - Expose via a `Runner` getter.
2. Register video files in `ScenarioArtifactReporter` (new suffix e.g. `-session.mp4` and a
   `videoFile` metadata key) so they auto-attach to ReportPortal.
3. Decide retention (always vs on-failure-only) and finalize-at-teardown handling.
4. Run `./gradlew validateConfigurationTemplates` after config changes.

## Pre-requisites

- Appium server reachable with screen-recording support.
- For iOS **simulator** recording: `ffmpeg` on the host.
- Disk / CI storage budget — videos are far larger than screenshots.
- Selenium CDP path (if chosen): Chromium-based browser.

## Clarifications needed (blocking)

1. Is **local Selenium** video in scope, or acceptable that Selenium relies on the cloud
   provider's own recording (local Selenium = no video)?
2. **Always record**, or only retain video **on failure**?
3. **Single property** for all engines/platforms, or **per-platform toggles**?
4. For cloud providers (BrowserStack / pCloudy / HeadSpin), prefer their **native video**
   over framework recording when running there?
5. Any **max duration / resolution / fps** constraints to enforce?

## Acceptance criteria (draft — finalize after clarifications)

- With `CAPTURE_SESSION_VIDEO=true`, a video artifact is produced per UI session for each
  in-scope engine and attached to the scenario report.
- With the property `false` (default), behaviour is unchanged and no video is produced.
- `./gradlew validateConfigurationTemplates` and `./gradlew test` remain green.

## Open design decision (cross-cutting)

Should this share a single "session capture" layer with the Web API Traffic Capture and
Socket Testing TODOs (common per-session hook in `BrowserDriverManager` /
`AppiumDriverManager` + common artifact publishing), or be implemented independently?
