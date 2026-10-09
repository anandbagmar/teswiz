## 1.0.44-SNAPSHOT (unreleased)
# Release Notes - teswiz v1.0.44-SNAPSHOT
_Previous release: v1.0.43_

## 🚀 Key Highlights & Enhancements

### 🧭 Cross-engine bounded element waits (`ElementWaiter`)
* **New `ElementWaiter` in `com.znsio.teswiz.runner`**, surfaced through three delegating methods on the `Driver` facade: `isElementVisible(By, int)`, `isElementPresentWithin(By, int)` and `waitTillTextIsPresent(By, String, int)`. Each waits up to a bounded number of seconds and **returns `false` on timeout instead of throwing** — the missing "is it there within N seconds?" boolean that every existing `waitTill*` lacked (they all throw on timeout). Additive and non-breaking: no existing signature changed. `By` stays the uniform locator currency, so these work across Selenium and (via `PlaywrightBy`) the Playwright engines.

### 🔌 Web-engine capability parity signalling
* **`WebCapability` gained `SHADOW_DOM` and `ALERTS`.** On `playwright-java`, `getShadowRoot()` and `switchTo().alert()` now throw a structured `WebEngineCapabilities` diagnostic naming the capability and the Playwright-native alternative, instead of a bare/absent failure — same pattern already used for windows/positioning.

### 📐 Consistent default window/viewport sizing (`WindowSizingResolver`)
* **New `WindowSizingResolver` is the single source of truth for the default viewport.** Selenium's hard-coded `1920x1080` headless fallback and the Playwright context-options viewport default now both resolve from it (`TESWIZ_DRIVER_VIEWPORT_WIDTH`/`HEIGHT`, default `1280x960`), so a sizing override applies consistently across engines. No config schema change.

### 🧱 Locator translation hardening (`PlaywrightLocator`)
* **`PlaywrightLocator.from(By)` now dispatches standard Selenium `By` subtypes through a single prefix→strategy lookup table** rather than an eight-branch `if` chain, and documents the `By` / `PlaywrightBy` contract. `PlaywrightBy`-native locators continue to translate directly from their structured fields. Behaviour is unchanged; the mapping is now locked by `PlaywrightLocatorTest`.

## 🧪 Tests
* `ElementWaiterTest` (6), `WindowSizingResolverTest` (3), `PlaywrightLocatorTest` (7) added. Full `clean test` green (441 tests, 0 failures).

---
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

## Unreleased
# Release Notes - teswiz (unreleased)
## 🚀 Key Highlights & Enhancements
### 👁️ Visual Subsystem — wait-until-visible and screen-layer consolidation
* **New `waitUntilVisualElementIsVisible*` methods on `VisualOcrBL`.** Four locator variants - by OCR text, by image template, by fallback text-or-image, and by fallback image-or-text - each with a default-budget overload and an overload taking an explicit maximum wait in seconds. They poll the existing engine-agnostic finders until the element is located on screen (non-null = visible) or the budget elapses, then assert visibility. The default budget derives from `VISUAL_ELEMENT_RETRY_ATTEMPTS x VISUAL_ELEMENT_RETRY_DELAY_SECONDS`; the inter-poll sleep is `VISUAL_ELEMENT_RETRY_DELAY_SECONDS`. Paired Gherkin steps (default and `within {int} seconds`) are added for all four variants. Works for Selenium, `playwright-java`, `playwright-ts` and Appium with no new TypeScript worker primitive, because the polling lives in the business layer and drives the already-abstract find operations.
* **The four `VisualOcrScreen` platform classes are consolidated behind a shared template.** `VisualOcrScreen` is now a pure abstract contract; a new `AbstractVisualOcrScreen` owns the single find → assert → highlight → screenshot(before) → act → wait → screenshot(after) choreography plus `parsePositionIndex`. The Android/iOS/web/playwright-java classes reduce to a constructor (Android/iOS additionally override an `afterTextEntry()` hook to dismiss the keyboard), removing roughly 1,400 lines of duplication. The contract stays abstract with a no-arg constructor so the dynamically generated `playwright-ts` bridge continues to work unchanged.

### 🧱 Visual Subsystem — encapsulation and SOLID clean-up
* **Gesture dispatch moved out of `VisualElement` into the `Driver` facade.** `VisualElement` is now a pure immutable value object (bounds + label) that forwards each gesture to the facade with its own centre/geometry; the engine-specific "how" lives in a new `VisualGestureDispatcher` (Appium touch → native coordinate input → Selenium `Actions` → synthesised DOM events). This removes the driver-type `instanceof` ladders and the `getInnerDriver()`/`getType()` coupling from the element.
* **`VisualElement`'s `WebElement` adapter extracted** into `VisualElementWebElementAdapter`, so the value object no longer also carries the Selenium-proxy responsibility.
* **`VisualOcrScreen` contract segregated** into role interfaces `VisualElementFinder`, `VisualElementActions`, and `VisualElementInspector`; the class remains the aggregate so callers and the `playwright-ts` bridge are unaffected.
* **Unsupported gestures now throw `UnsupportedVisualGestureException`** instead of logging and continuing. A swipe/long-press on a plain Selenium web driver (no touch, no native coordinate input) and `zoom`/`pinch` on any engine fail loudly rather than passing against an unchanged app. **Parity note:** `click`/`doubleClick`/`hover`/`sendKeys` work on all engines; `swipe`/`longPress` require touch (Appium) or native coordinate input (Playwright), so parity is intentionally not provided on a mouse-only Selenium web driver.
* **Migrated the last four Log4j 1.x files to Log4j2** (`SessionContext`, `TestExecutionContext`, `CucumberScenarioListener`, `CucumberScenarioReporterListener`) and removed the `log4j1.compatibility` shim. No source now imports `org.apache.log4j.*`.

## 1.0.4305
# Release Notes - teswiz v1.0.4305
## 🚀 Key Highlights & Enhancements
### 👁️ Visual Subsystem — self-clearing gestures and diagnostic captures
* **Visual gestures now clean up their own highlight overlay.** `VisualElement.click()`, `doubleClick()`, `hover()`, `longPress()`, `sendKeys()`, `swipe()` and `dragAndDropTo()` now clear the highlight they draw, via a `try/finally` around the gesture body (each body extracted into a `performX()` helper). Previously the highlight was removed only by the `VisualOcrScreen.clickVisualElementBy*` ritual, so any caller that located an element and acted on it directly (e.g. `findVisualElementByImage(...).click()`) left the orange highlight box painted on the page. On a canvas with the SPIN control at the same position across screens, that leftover overlay was then captured in the next locate screenshot and masked the template, breaking the subsequent match. The cleanup is driver-agnostic — `Driver.clearHighlight()` already no-ops safely on Appium — so Selenium, `playwright-java`, `playwright-ts` and Appium all benefit.
* **Before/After visual captures are now plain diagnostic screenshots, not Applitools baseline checks.** Across all four `VisualOcrScreen` implementations (web, playwright-java, iOS, Android) the per-action `visually.checkWindow(...)` Before/After calls are replaced with `visually.takeScreenshot(...)`, and the now-redundant explicit `driver.clearHighlight()` lines are removed (the gesture self-clears). These captures were always meant as "what did the action do" evidence, not product visual-regression assertions; routing them through `takeScreenshot` keeps the framework-injected highlight out of consumers' Applitools baselines and stops incidental per-action pixel changes raising false visual diffs.
* **Breaking change for visual-regression users:** the ~60 per-action `checkWindow` checkpoints across the four Screen classes are gone. Consumers who relied on those as regression gates will no longer get an Applitools comparison on each visual action; add explicit `checkWindow` calls where a product-UI baseline is genuinely wanted.

## 1.0.42
# Release Notes - teswiz v1.0.41

## 🚀 Key Highlights & Enhancements

### 🌐 Playwright Engine (`playwright-java` & `playwright-ts`)
* **Maximized Window Mode Parity**: Fixed viewport resolution in `PlaywrightBrowserConfigResolver` to omit fixed viewport size (`1280x960`) when `"maximize": true` or `--start-maximized` is configured. This allows Playwright Java and TS engines to honor full-screen maximized windows without fixed viewport emulation overriding `--start-maximized`.
* **Dynamic Window Controls**: Updated `PlaywrightJavaWebDriver.manage().window().maximize()` to expand viewport to screen available size (`window.screen.availWidth` x `availHeight`), and `getSize()` to evaluate actual inner window dimensions when viewport is `null`.
* **Navigation & Timeout Resiliency**: Added automatic fallback to `COMMIT` navigation in `PlaywrightJavaWebDriver` and Node worker script (`worker.mjs`) when slow third-party scripts cause `DOMCONTENTLOADED` timeouts.
* **Synchronized Page Transitions**: Wrapped link clicks with `page.waitForNavigation()` in Playwright Java screen handlers to ensure smooth navigation transitions.
* **Visual Testing Parity**: Disabled CSS animations during screenshot capture and aligned navigation wait strategies to eliminate visual check flakes.

### 👁️ Visual Subsystem & OCR Logging
* **OCR Region Log Formatting**: Improved logger output in `Visual.java` to display `[entire screen]` instead of raw Java `null` when performing OCR or image template detection without an explicit bounding box crop.

### 📡 API Subsystem & Telemetry
* **Unified Engine Resolution**: Refactored `ApiService` and `RunTelemetry` to consume API engine settings uniformly via `Runner.getApiEngine()` (`rest-assured`, `playwright-java`, `playwright-ts`).
* **API Policy Compliance**: Passed explicit `User-Agent` headers in `WeatherAPIBL` to satisfy OpenStreetMap/Nominatim policy requirements and eliminate HTTP `403 Forbidden` errors.

### 🧹 Configuration & Repository Cleanups
* **Configuration Normalization**: Removed redundant `# Historical properties...` and `# Project/provider-specific properties...` comment lines across all `configs/**/*.properties` files.
* **Gradle Plugin Modernization**: Updated deprecated `com.github.ben-manes.versions` to `io.github.ben-manes.versions` in `build.gradle`.
* **Publish Script Fix**: Resolved bash single-quote syntax errors on line 320 in `publishRelease.sh`.
* **Cucumber Deprecation Cleanup**: Removed deprecated `AbstractTestNGCucumberTests` inheritance from Cucumber runners (`RunCukes` and `RunTestCukes`).

### 📦 Dependencies & Compatibility
* **ReportPortal Cucumber 8 Support**: Upgraded `agent-java-cucumber7` to `5.4.10` for full Cucumber 8 and TestNG compatibility.
* **Linux CI Tesseract Support**: Pinned `tess4j` to `5.11.0` to prevent `libtesseract` missing symbol errors (`pixBackgroundNormTo1MinMax`) on Linux runners.

---

## 🛠️ Changed Files Summary

* `build.gradle` — Version bump to `1.0.41`, plugin ID update, and dependency upgrades.
* `src/main/java/com/znsio/teswiz/config/browser/PlaywrightBrowserConfigResolver.java` — Maximized viewport resolution logic.
* `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightJavaDriverManager.java` — Handled `null` viewport size in context options.
* `src/main/java/com/znsio/teswiz/web/playwright/PlaywrightJavaWebDriver.java` — `maximize()`, `getSize()`, and navigation fallback to `COMMIT`.
* `playwright/worker.mjs` — Maximized viewport support and navigation fallback in Node worker.
* `src/main/java/com/znsio/teswiz/runner/Visual.java` — Formatted `null` OCR region logging output as `[entire screen]`.
* `src/main/java/com/znsio/teswiz/services/ApiService.java` & `src/main/java/com/znsio/teswiz/runner/Runner.java` — Refactored API engine lookup.
* `src/test/java/com/znsio/teswiz/businessLayer/weatherAPI/WeatherAPIBL.java` — Added `User-Agent` header for Nominatim API endpoints.
* `publishRelease.sh` — Fixed quote escaping syntax error.
* `configs/**/*.properties` — Cleaned up redundant comment headers.

---

## 📋 Installation & Upgrade

To use version `1.0.41` in your Gradle project:

```groovy
dependencies {
    implementation 'com.github.anandbagmar:teswiz:1.0.41'
}

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

## 1.0.35
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

## 1.0.35
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

## 1.0.34
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

## v1.0.27
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

# Changelog

## v1.0.26

### Driver enhancements

- Added a reusable page source dump helper that writes `dom-dump-<index>.html` for web and `hierarchy-dump-<index>.xml` for Android/iOS into the active scenario log directory.
- Added per-scenario dump indexing so repeated calls create sequential files that reset with each scenario.
- Reorganized driver import statements for clarity.

### Tests

- Updated the TheApp invalid-login scenario to execute the second login attempt again so the web flow covers the reusable invalid-login path.

## v1.0.25

### Runner

- Auto-include the `com.znsio.teswiz.steps` package as a second `--glue` entry so reusable built-in steps (`FigmaSteps`, `CommonSteps`, etc.) are discoverable in consuming projects without extra glue configuration.

### Driver enhancements

- Added a maximum scroll attempts limit and improved element visibility checks.

### Figma and Applitools

- Added the missing step to navigate to the application page in the Figma design scenario.
- Updated UFG device emulation to iPhone 15 Pro in the UFG configuration.
- Removed unused driver imports and driver creation call in the Figma step.

## v1.0.24

### Driver enhancements

- Enhanced click methods and context switching functionality in the driver.

### Build and dependency updates

- Updated `appium-xcuitest-driver` to version `11.13.0`.
- Updated Gradle to `9.6.0`.
- Updated all Java dependencies.
- Updated GitHub Actions to v7.
- Refreshed npm/node packages.

## v1.0.23

### Build and dependency updates

- Changed dependency download behavior so `forceUpdate` now defaults to `false`.
- Added a Gradle override so `-PforceUpdate=true` forces a fresh jar download when needed.
- Updated the README guidance to document the Gradle property-based override.
- Kept `build.gradle.sample` aligned with the new default and override flow.

## v1.0.22

### Build and dependency updates

- Updated the Gradle wrapper to `9.5.1`.
- Improved Gradle launcher script compatibility in `gradlew` and `gradlew.bat`.
- Refreshed npm dependencies in `package.json` and regenerated `package-lock.json`.
- Updated Gradle plugin and dependency versions in `build.gradle`.

## v1.0.21

### Visual testing

- Fixed Applitools mobile capability setup for Appium visual runs so it uses the initialized Applitools configuration map correctly.
- Scoped Applitools mobile capability injection to runs where both `IS_VISUAL=true` and `useNML=true`.
- Simplified proxy settings initialization in `Visual.addMobileCapabilitiesToTestContextForVisualTesting`.

## v1.0.20

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
