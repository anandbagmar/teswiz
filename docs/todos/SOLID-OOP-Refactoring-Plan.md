# teswiz SOLID/OOP Refactoring Plan (TODO)

> Status: **Not started** — captured for later pickup.
> Source: codebase-wide SOLID/OOP review. Ordered lowest-risk / highest-value first.

A sequenced, low-risk plan to pay down the SOLID/OOP debt found in the codebase review. Ordered **lowest-risk / highest-value first**, so each step is independently shippable and the risky God-class work comes only after the cheap wins and safety nets are in place.

## Judgement notes (read first)

- **Phase 4, Step 4.1 is NOT a breaking change (revised).** The 26 `Visual` find pass-throughs on `Driver` are pre-existing stable public API and are **kept** — the earlier `@Deprecated` is reverted; no removal in this phase. Any hard removal is a separate, pre-announced major-version decision. **Guiding rule: no hard removals — deprecate only with explicit maintainer sign-off; default to keeping stable APIs so consumers adopt with zero forced edits.** 4.1 also newly extracts `ElementWaiter` and adds additive bounded waits + alert/shadow-root parity (see Phase 4).
- **Phases 1–3 are safe to interleave or even stop after.** Each delivers value independently. Phases 0–3 get most of the SOLID benefit without the biggest risk, and leave the two God classes meaningfully smaller even if Phase 4 is deferred indefinitely.
- **Phase 4 should come last.** It has the widest blast radius and is materially safer once the finder is extracted (Phase 1), config is injectable (Phase 3), and driver creation is polymorphic (Phase 2).

## Guiding principles

- **One concern per commit.** Each step below is a standalone commit (or small series) with its own verification. No mixing refactors.
- **Behaviour-preserving.** These are refactors, not feature changes. Public API that tests/steps depend on stays intact unless a step explicitly calls out a (compatible) change.
- **Parity across engines.** Everything must keep working at par for web (Selenium, Playwright-Java, Playwright-TS) and Appium. Any place parity is *not* required is called out explicitly.
- **The gate for every step:** `./gradlew clean test` green (baseline at time of review: 536 passed, 0 failed), plus `./gradlew validateConfigurationTemplates`. Add targeted unit tests where a seam becomes newly testable.
- **The root cause we keep attacking:** "polymorphism by `switch`/`instanceof` on a stringly-typed discriminator." Replacing that with real polymorphism fixes OCP, DIP, and LSP together.

## Risk / effort legend

| Symbol | Meaning |
|---|---|
| 🟢 | Low risk, small blast radius |
| 🟡 | Medium risk, contained blast radius |
| 🔴 | High risk, wide blast radius — do last, behind the safety nets |

---

## Phase 0 — Safety nets & quick wins (🟢)

Cheap, isolated changes that reduce risk for everything after. No architectural change.

### Step 0.1 — Correctness nits
- `CucumberScenarioListener.java:~151` — `catch (Exception e) { ExceptionUtils.getStackTrace(e); }` discards the stack trace. → `LOGGER.warn("...", e);`
- `Visual.java` (~1272/1293/1328) and any other `catch (InterruptedException ignored) {}` → restore the flag with `Thread.currentThread().interrupt();`
- `Setup.getIntegerValueFromConfigs` (~836) — unboxing `configsInteger.get(key)` NPEs on a missing key. → return a safe default (or `Optional`/boxed) and document.
- Audit static `Map`-returning getters (`Runner.getApplitoolsConfiguration()`, `reporting/*`) — return unmodifiable copies if they currently hand out the live map.

**Verify:** unit tests + full suite. **Commit:** "Fix silent catches, interrupt handling, and unsafe config/int lookups".

### Step 0.2 — Provider resolver registries (OCP) 🟢
- `MobileExecutionProviderResolver.java:14–26` and `WebExecutionProviderResolver.java:14–22`: replace the string `switch` with a `Map<String, Supplier<Provider>>` keyed on each provider's `name()`, default `Local*`.
- Parity: unchanged — all providers (BrowserStack/LambdaTest/HeadSpin/pCloudy/Local) behave identically; adding a provider no longer edits a switch.

**Verify:** add a small unit test resolving each known name + an unknown name → Local. Full suite. **Commit:** "Resolve execution providers via registry instead of switch".

### Step 0.3 — `VisualBy` duplication 🟢
- Hoist the triplicated `if (context instanceof Driver)` blocks (`VisualBy.java:99–147`) into one shared helper/base.

**Verify:** `VisualSubsystemTest` (already covers `VisualBy`). **Commit:** "De-duplicate VisualBy driver-context dispatch".

---

## Phase 1 — Extract `VisualFinder` from `Visual` (🟡)

Highest-value God-class win: isolates the ~24-method OCR find surface and **breaks the `Driver ↔ Visual` back-reference cycle**.

### Step 1.1 — Introduce `OcrEngine` / `ImageMatcher` interfaces (DIP) 🟢
- Extract interfaces for `OcrService` / `ImageRecognitionService` (currently concrete, static-only). Keep the existing classes as the default implementations.
- Rationale: makes the finder unit-testable with a fake engine and lets the Tesseract/OpenCV backend be swapped.

**Verify:** full suite (behaviour identical — just an interface in front of statics). **Commit:** "Introduce OcrEngine and ImageMatcher abstractions".

### Step 1.2 — Extract `VisualFinder` 🟡
- Move `findByText/findByImage/findByTextOrImage/findByImageOrText`, their `findAll*` and `findRelative*` variants, `filterAndSelectClosestRelative`, `captureScreenshotBytes`, `verifyOcrEnabled`, retry helpers out of `Visual` into a new `VisualFinder` that depends on the screenshot source + the injected `OcrEngine`/`ImageMatcher` (not on the Applitools fields).
- `Visual` keeps delegating its public find methods to the finder (compatibility), and `Driver`'s 26 find pass-throughs keep working unchanged.
- `filterAndSelectClosestRelative`'s `switch (SpatialDirection)` can move onto `SpatialDirection` itself (each direction knows its own predicate) — optional OCP improvement.

**Verify:** `VisualSubsystemTest`, `GeneratedScreenParityTest`, `PlaywrightTs*`, transportnsw; full suite. Add `VisualFinder` unit tests with a fake `OcrEngine`. **Commit:** "Extract VisualFinder from Visual".

**Parity note:** finders already work on all engines; this is pure relocation.

---

## Phase 2 — `DriverManager` interface + registry (🟡)

Collapse the three stacked creation switches into polymorphic dispatch.

### Step 2.1 — Define the `DriverManager` contract 🟡
- Introduce `DriverManager` with a uniform `createSession(...)`/`close(...)` surface. Use `PlaywrightJavaDriverManager` (already DI-constructed, instance-based, testable) as the template.
- Normalize `AppiumDriverManager` to return the same `WebDriverSessionResult` shape as the web managers (today it returns `Driver`), so dispatch tables align.

**Verify:** full suite (all platforms/engines). **Commit:** "Introduce DriverManager abstraction".

### Step 2.2 — Replace creation/close switches with a registry 🟡
- `Drivers.createDriverForPlatform` (switch on `Platform`, 112–139) and `BrowserDriverManager` (switch on `WebEngine`, 48–72, create+close) → a `Map<Platform, DriverManager>` / `Map<WebEngine, DriverManager>` registry.
- Also move `Drivers.attachLogsAndCloseDriver`'s switch-on-`driver.getType()` (279–296) onto the same dispatch.

**Verify:** full suite; confirm BrowserStack/local paths for each engine. **Commit:** "Dispatch driver creation/close via registry instead of switch".

**Parity note:** required and preserved for Selenium / Playwright-Java / Playwright-TS / Appium. Electron continues routing to Selenium.

---

## Phase 3 — Extract `TeswizConfiguration` from `Setup` (🟡→🔴)

Break the static-utility God object; make config injectable and testable.

### Step 3.1 — `TeswizConfiguration` value/holder 🟡
- Wrap the three parallel maps (`configs`, `configsBoolean`, `configsInteger`) in an injectable `TeswizConfiguration` with safe typed getters (no NPE on missing keys).
- `Setup` keeps its static facade initially, delegating to a `TeswizConfiguration` instance, so callers (`Runner`, managers) are untouched in this step.

**Verify:** full suite. **Commit:** "Introduce TeswizConfiguration holder behind Setup".

### Step 3.2 — Split `Setup`'s responsibilities 🔴
- Extract (incrementally, one per commit): `ConfigLoader`, `PlatformTagResolver` (replace the `multiuser-*` if/else chain, 538–575, with a tag → (Platform, launchName) table), `ApplitoolsConfigFactory`, `CucumberArgsBuilder`, `ReportPortalEnvironmentPublisher` (the `System.setProperty` block).
- Each extraction is instance-based and unit-testable; `Setup` becomes a thin orchestrator.

**Verify:** full suite after each extraction; this is where the config class-load ordering trap (managers reading `Runner.getMaxNumberOf*Drivers()` at static-init) gets removed. **Commits:** one per extracted collaborator.

---

## Phase 4 — Decompose `Driver` and `Visual` God classes (🔴)

Widest blast radius — intentionally last, behind all the safety nets.

### Step 4.1 — Extract from `Driver` 🔴
- `MobileGestures` (takes the `AppiumDriver`): all ~28 Appium gesture methods + private math helpers; removes the pervasive `(AppiumDriver)` casts and the platform switches (`putAppInBackgroundFor`, `pushFileToDevice`, `relaunchApp`, clipboard).
- `ElementHighlighter` (web JS): `clearHighlight`/`highlightElement`/`highlightVisualElement` + `activeHighlightBounds` + the JS string literals. *(Done.)*
- `ElementFinder` + the `decorateElement` proxy.
- **`ElementWaiter`** — the bounded synchronisation currently scattered on `Driver` (`waitTillElementIsVisible/Present/Invisible`, `waitForClickabilityOf`, `waitTillVisibilityOfAllElements`, `waitTillPresenceOfAllElements`, `waitForAlert`). `Driver` keeps thin delegating wait methods.
- **Keep the 26 `Visual` find pass-throughs** on `Driver` (revert the earlier `@Deprecated`; do NOT delete) — pre-existing stable public API. No breaking change.
- **Additive new capability (the one behaviour-add in this phase):** `ElementWaiter` adds bounded, non-throwing `isElementVisible(By, int seconds)`, `isElementPresentWithin(By, int seconds)`, `waitTillTextIsPresent(By, String, int seconds)` — closes the "no bounded non-throwing visibility/text probe on any engine" gap. All take `By` (the uniform locator currency; Playwright locators via `PlaywrightBy`), so one implementation serves every engine.
- **Capability parity (LSP fix):** make `waitForAlert` + `getShadowRoot` uniform across engines — today `waitForAlert` throws on PW-Java / no-ops on Appium, and `getShadowRoot` throws on PW-Java. Implement on PW-Java or surface a single capability-unsupported signal (like `NativeCoordinateInput`); no divergent per-engine throws.
- `Driver` becomes a thin facade composing these.

**Parity note:** mobile gestures are Appium-only by nature — parity with web is **not** required for `MobileGestures`; highlighting is web-only (no-op on Appium) — also not required to be at parity.

**Verify:** full suite; this touches many screen/BL classes, so expect broad recompilation.

### Step 4.2 — Decompose `Visual` behind a `VisualEngine` strategy 🔴 — **DEFERRED**
> **Deferred (revisit on demand).** Reshapes the public `Visual` facade — `checkWindow(...)` is the one `Visual` method downstream screens call (e.g. casino web screens), and this step is where the LSP `checkWindow` rework lives. All risk to a consumed API, no near-term benefit. Do only if `Visual` becomes actively painful to change.
- Extract `PdfVisual`, `WebVisual`, `AppVisual` (the Playwright web path folds into `WebVisual`) behind a `VisualEngine` interface; `Visual` becomes a facade that composes the one engine relevant to the session.
- This removes the "all three engines constructed every time" smell (156–180), the `handleTestResults` driverType switch (~1059), and the LSP problem where a PDF-constructed `Visual` NPEs on `checkWindow`.

**Verify:** full suite incl. PDF, Applitools-disabled, Playwright-TS paths.

---

## Dependency order (what unblocks what)

```
Phase 0  (independent, do first)
   │
Phase 1  VisualFinder  ── needs 1.1 (OcrEngine) before 1.2
   │                      breaks Driver↔Visual cycle → eases Phase 4
Phase 2  DriverManager registry  (independent of Phase 1)
   │
Phase 3  TeswizConfiguration  ── 3.1 before 3.2; eases testing in Phase 4
   │
Phase 4  Driver / Visual decomposition  (do LAST; benefits from 1,2,3)
```

Phases 1, 2, 3 are largely independent and can be interleaved. **Phase 4 should come last.**

## Explicit parity callouts

- **Required & preserved everywhere:** finders, click/doubleClick/hover/sendKeys, waits, screen actions, waitUntilVisible, config resolution, driver creation/close — for Selenium, Playwright-Java, Playwright-TS, and Appium.
- **Parity NOT required (by nature):** `MobileGestures` (Appium-only touch gestures), DOM/JS highlighting (web-only, no-op on Appium), `swipe`/`longPress` on a plain Selenium web driver (already throws `UnsupportedVisualGestureException`), `zoom`/`pinch` (unimplemented on all engines, throw uniformly).

## Verification checklist (run per step)

1. `./gradlew clean test` → green (baseline: 536 passed, 0 failed).
2. `./gradlew validateConfigurationTemplates` → green.
3. New/updated unit tests for any newly-created seam (interfaces, registries, extracted collaborators).
4. Spot-check the Playwright-TS bridge tests (`GeneratedScreenParityTest`, `PlaywrightTs*`) after any `screen`/`Driver`/`Visual` change — the ByteBuddy bridge is sensitive to contract shape.
5. No new SonarQube S2629 / S3457 / broad-catch regressions.

## Out of scope (deliberately)

- Behaviour changes / new features.
- The build-number bump (owned by maintainer; set at release).
- The `log4j-1.2-api` bridge (already removed).
- Rewriting the Applitools/Playwright SDK integration beyond introducing seams.

## Suggested commit sequence (one line each)

1. Fix silent catches, interrupt handling, and unsafe config/int lookups
2. Resolve execution providers via registry instead of switch
3. De-duplicate VisualBy driver-context dispatch
4. Introduce OcrEngine and ImageMatcher abstractions
5. Extract VisualFinder from Visual
6. Introduce DriverManager abstraction
7. Dispatch driver creation/close via registry instead of switch
8. Introduce TeswizConfiguration holder behind Setup
9. Split Setup into ConfigLoader / PlatformTagResolver / ApplitoolsConfigFactory / CucumberArgsBuilder / ReportPortalEnvironmentPublisher (one commit each)
10. Extract MobileGestures / ElementHighlighter / ElementFinder from Driver; drop Visual find pass-throughs
11. Decompose Visual behind a VisualEngine strategy (PdfVisual / WebVisual / AppVisual)

---

## Phase 5 — Unify & clean up `browser_config` across engines (🟡→🔴)

> Status: **Not started** — captured for later pickup.
> Source: `browser_config` sizing/consistency review (Selenium, Playwright-Java, Playwright-TS).

Independent of Phases 0–4. Benefits from Phase 3 (`TeswizConfiguration`) being in place for the viewport runtime config, but does not require it. **This is a schema contract change**, so it carries a user-facing migration — treat it like Phase 4.1's public-API change: its own review, its own release note.

### The problem we're attacking

"Sizing (and much else) is specified multiple times in different dialects, and each engine reads a different subset." Concretely, from the current code:

- Window sizing is specified 2–3 times: top-level `maximize` (boolean) + `arguments:["--window-size=W,H"]` (Selenium + PW launch arg) + `playwright.contextOptions.viewport` (PW render surface). No single source of truth.
- `SeleniumDriverManager` ignores the `playwright` block entirely; that block is also **unvalidated** (schema defines neither `playwright` nor `safari`, relying on `additionalProperties:true`).
- Headless sizing diverges: Selenium hard-codes `1920x1080` (`manageWindowSizeAndHeadlessMode`); Playwright uses the runtime viewport. Same config → different result per engine.
- Two coexisting dialects: legacy top-level (`arguments`, `headlessOptions`, `binary`, `noProxy`) vs nested `playwright.{launchOptions,contextOptions}`.
- pw-java (`buildContextOptions`/`buildLaunchOptions`) uses an explicit allow-list; pw-ts (`worker.mjs`) passes `contextOptions` through wholesale → a key can silently work in one and not the other.
- Schema asymmetry: `chrome.required` includes `maximize`/`acceptInsecureCerts`/`verboseLogging`; `firefox.required` does not — yet Selenium reads them with strict `getBoolean(...)`, so a schema-valid firefox block can throw at runtime.
- Dead code: `addWindowSizeToChromeOptions` (AWT `Toolkit` screen size) is only used on the electron path; the web path never calls it. `w3c` is hard-coded, not from config.

### Target contract (engine-neutral, grouped)

One schema; specify intent once, each driver translates to its native settings.

- `window.mode`: `maximized` | `fixed` | `fullscreen` (default `maximized`) — the single sizing control. Size for `fixed` always comes from `TESWIZ_DRIVER_VIEWPORT_WIDTH/HEIGHT` (one source of truth, no magic numbers).
- Flattened neutral keys: `headless`, `arguments`, `headlessArguments` (was `headlessOptions.include`), `acceptInsecureCerts`, `verboseLogging`, `browser.{version,channel,executablePath}`, `context.{ignoreHTTPSErrors,locale,timezoneId,userAgent,deviceScaleFactor,extraHTTPHeaders}`.
- Engine-specific escape hatches: `selenium.{excludeSwitches,preferences,excludedSchemes,firefoxProfile,electronAppLoadingPage,electronAppLoadTime}` and `playwright.{launchArgs,contextOptions}` (raw passthrough for advanced use).
- `maximize` boolean and `headlessOptions` object are removed from the canonical schema (handled by `window.mode` / `headless` + `headlessArguments`).
- `acceptInsecureCerts` and `context.ignoreHTTPSErrors` resolve to the same value so Selenium and PW agree on TLS behaviour.

Sizing resolution matrix (shared `WindowSizingResolver`; VW/VH = `TESWIZ_DRIVER_VIEWPORT_WIDTH/HEIGHT`):

| `window.mode` | Selenium (headed) | Selenium (headless) | PW-Java / PW-TS |
|---|---|---|---|
| `maximized` | `window().maximize()` | `setSize(VW,VH)` | `--start-maximized` + `viewport:null` |
| `fixed` | `--window-size=VW,VH` + `setSize(VW,VH)` | same | `--window-size=VW,VH` + `viewport:{VW,VH}` |
| `fullscreen` | `window().fullscreen()` / `--start-fullscreen` | `setSize(VW,VH)` | `--start-fullscreen` + `viewport:null` |

Invalid viewport values already fail fast at startup via `TeswizRuntimeConfiguration` (positive-int validation), identically for all three engines — no new validation needed.

### Step 5.1 — `WindowSizingResolver` (DIP/SRP) 🟢
- New class in `config/browser`: input = normalized `window.mode` + runtime viewport; output = `{mode,width,height}`. The single sizing authority both engines call.

**Verify:** `WindowSizingResolverTest` (new). Full suite. **Commit:** "Introduce WindowSizingResolver as single sizing authority".

### Step 5.2 — `BrowserConfig` model + `BrowserConfigNormalizer` (SRP) 🟡
- Typed, engine-neutral model parsed once; both `SeleniumDriverManager` and `PlaywrightBrowserConfigResolver` consume the model instead of raw `JSONObject`. Removes the strict-`getBoolean` landmines and the pw-java/pw-ts drift (one normalizer feeds both). Normalizer parses the unified shape **and** legacy (for in-memory migration, Step 5.6).

**Verify:** `BrowserConfigNormalizerTest` (new; also first real coverage of Selenium-side config reading, which is currently untested). Full suite. **Commit:** "Introduce typed BrowserConfig model and normalizer".

### Step 5.3 — Rewrite `BrowserConfigSchema.json` 🟡
- Unified shape; engine-neutral keys documented/required; `additionalProperties:true` confined to the `selenium`/`playwright` escape hatches. Define `safari` and `playwright` explicitly. Fix the chrome/firefox `required` asymmetry.

**Verify:** schema round-trip test — all committed configs validate. **Commit:** "Rewrite BrowserConfigSchema to unified contract".

### Step 5.4 — Wire the Playwright resolver 🟡
- `PlaywrightBrowserConfigResolver` consumes the model; replace the inline `isMaximized`/viewport block with `WindowSizingResolver`; drop legacy `headlessOptions`/`maximize` reads. Align pw-java `buildContextOptions` with pw-ts passthrough; verify `PlaywrightWorkerManager.toJson` + `worker.mjs` payload shape unchanged.

**Verify:** rework `PlaywrightBrowserConfigResolverTest` (7 tests assert the old shape + hard-coded `1280x960`/`1440x900`). Full suite + Playwright-TS bridge tests. **Commit:** "Resolve Playwright browser config via unified model".

### Step 5.5 — Wire `SeleniumDriverManager` 🟡
- Consume the model; replace `getBoolean(MAXIMIZE)` + the `1920x1080` literal in `manageWindowSizeAndHeadlessMode` with `WindowSizingResolver` output; feed `--window-size` for `fixed`; move Chrome prefs/excludeSwitches reads under the `selenium` hatch; delete dead `addWindowSizeToChromeOptions` from the web path (keep electron window handling).

**Verify:** full suite; manual spot-check headed + headless for `maximized` and `fixed`. **Commit:** "Resolve Selenium browser config via unified model".

### Step 5.6 — Migration path (reuse existing machinery) 🔴
- Generalize `PlaywrightBrowserConfigMigrator` from "add playwright block" to "legacy → unified" (`maximize`→`window.mode`, strip `--window-size`/`--start-maximized` from `arguments`, `headlessOptions`→`headless`+`headlessArguments`, `binary`/`executablePath`→`browser.executablePath`, `browserVersion`→`browser.version`, prefs/excludeSwitches/firefoxProfile/electron→`selenium`).
- Rename artifact to `<name>-unified.json`; update `PlaywrightBrowserConfigMigrationReporter` summary wording.
- **Non-breaking for one release:** a pre-validation shim detects a legacy config, migrates it in-memory so the current run works, prints the notice, and writes the unified file. Hard cutover (reject legacy) deferred to the next release, pre-announced.
- Optional `./gradlew migrateBrowserConfig` task to migrate files in place.
- Rewrite the 3 committed configs (`configs/browser_config.json`, `configs/second_browser_config.json`, `src/main/resources/default_browser_config.json`) + build copy into the unified shape, preserving each file's current behaviour (e.g. `second_browser_config` chrome `maximize:false` → `window.mode:"fixed"`).

**Verify:** rework `PlaywrightBrowserConfigMigrationReporterTest` (3 tests assert artifact name + message strings). Full suite. **Commit(s):** "Migrate legacy browser_config to unified contract (with in-memory compat)" + "Rewrite committed browser_config files to unified contract".

### Parity note
Required & preserved for Selenium, Playwright-Java, Playwright-TS. The one deliberate behaviour change: **headless maximized size now follows `TESWIZ_DRIVER_VIEWPORT_WIDTH/HEIGHT` instead of the old Selenium `1920x1080` literal** — call this out in `Changelog.MD`. Electron keeps its existing window handling.

### Per AGENTS.md
`BROWSER_CONFIG_FILE` is currently set in every `configs/**/*.properties` but missing from the canonical `configs/teswiz/teswiz_config.properties.template`. Add it to the template as part of this phase, then run `./gradlew validateConfigurationTemplates`.

### Suggested commit sequence (append to the global list)

12. Introduce WindowSizingResolver as single sizing authority
13. Introduce typed BrowserConfig model and normalizer
14. Rewrite BrowserConfigSchema to unified contract
15. Resolve Playwright browser config via unified model
16. Resolve Selenium browser config via unified model
17. Migrate legacy browser_config to unified contract (in-memory compat) + rewrite committed configs + template
