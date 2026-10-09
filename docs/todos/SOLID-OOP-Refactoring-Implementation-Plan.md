# teswiz SOLID/OOP Refactoring — Implementation Plan

> Companion to [`SOLID-OOP-Refactoring-Plan.md`](./SOLID-OOP-Refactoring-Plan.md).
> That document is the *what & why* (sequencing, risk, design). This document is the
> *how*: a concrete, execution-ready playbook — branch strategy, per-step file lists,
> the seam/test to add, acceptance criteria, and a tracking checklist.
>
> **Baseline at pickup:** `Visual.java` 1536 lines, `Driver.java` 1440 lines,
> `Setup.java` 871 lines. Test gate: `./gradlew clean test` green (536 passed, 0 failed)
> + `./gradlew validateConfigurationTemplates` green.

## How to use this plan

- **One concern per commit.** Every numbered step below maps to exactly one commit (Phase 3.2 and 5.6 expand to several). Use the commit messages from the source plan's "Suggested commit sequence".
- **Each step is independently shippable.** Open a PR per phase (or per step for 🔴 steps). Do not batch a 🔴 step with anything else.
- **The gate is non-negotiable.** Run the [Per-step verification](#per-step-verification-run-every-time) block before every commit. A step is not "done" until the gate is green and the step's acceptance criteria are met.
- **Stop points are real.** Phases 0–3 deliver most of the SOLID value with low risk. It is valid to pause after any phase. Phase 4 and Phase 5.6 are the only user-facing changes and each needs its own review + release note.

## Branch & PR strategy

```
main
 └── solid-oop-refactoring-implementation-plan      (this planning branch — docs only)
      ├── refactor/phase-0-safety-nets              (PR: Phase 0, steps 0.1–0.3)
      ├── refactor/phase-1-visual-finder            (PR: Phase 1, steps 1.1–1.2)
      ├── refactor/phase-2-driver-manager           (PR: Phase 2, steps 2.1–2.2)
      ├── refactor/phase-3-teswiz-config            (PR: Phase 3, steps 3.1–3.2)
      ├── refactor/phase-4-god-class-decomp         (PR: Phase 4 — split 4.1 / 4.2 if large)
      └── refactor/phase-5-browser-config-unify     (PR: Phase 5, steps 5.1–5.6)
```

- Phases 1, 2, 3 are independent and may proceed in parallel on separate branches.
- Phase 4 branches off `main` only after 1, 2, 3 have merged (it benefits from all three).
- Phase 5 is independent of 0–4; benefits from (but does not require) Phase 3.
- Each phase PR targets `main`. 🔴 PRs (4.1, 5.3–5.6) get an explicit reviewer sign-off and a `Changelog.MD` entry.

## Per-step verification (run every time)

```bash
./gradlew clean test                      # green — baseline 536 passed, 0 failed
./gradlew validateConfigurationTemplates  # green
```

Plus, per step:
1. New/updated unit tests for any newly-created seam (interface, registry, extracted collaborator).
2. Spot-check Playwright-TS bridge tests (`GeneratedScreenParityTest`, `PlaywrightTs*`) after any `screen` / `Driver` / `Visual` contract change — the ByteBuddy bridge is shape-sensitive.
3. No new SonarQube regressions (S2629 parameterized logging, S3457, broad-catch).

---

## Phase 0 — Safety nets & quick wins 🟢

**Branch:** `refactor/phase-0-safety-nets` · **Risk:** low · **Prereq:** none

### Step 0.1 — Correctness nits
| | |
|---|---|
| **Files** | `CucumberScenarioListener.java` (~151), `Visual.java` (~1272/1293/1328), `Setup.getIntegerValueFromConfigs` (~836), static map getters in `Runner` / `reporting/*` |
| **Change** | Swallowed stack trace → `LOGGER.warn("...", e)`; `catch (InterruptedException ignored)` → `Thread.currentThread().interrupt()`; guard `configsInteger.get(key)` unboxing with a safe default; return unmodifiable copies from live-map getters |
| **Seam/test** | Unit test for `getIntegerValueFromConfigs` with a missing key (asserts default, no NPE) |
| **Accept** | No swallowed exceptions on these paths; missing-key int lookup returns default; callers can't mutate internal maps; full gate green |
| **Commit** | `Fix silent catches, interrupt handling, and unsafe config/int lookups` |

### Step 0.2 — Provider resolver registries (OCP)
| | |
|---|---|
| **Files** | `MobileExecutionProviderResolver.java` (14–26), `WebExecutionProviderResolver.java` (14–22) |
| **Change** | Replace string `switch` with `Map<String, Supplier<Provider>>` keyed on each provider's `name()`, default `Local*` |
| **Seam/test** | Unit test resolving each known provider name + an unknown name → `Local` |
| **Accept** | Adding a provider no longer edits a switch; all providers behave identically; parity unchanged |
| **Commit** | `Resolve execution providers via registry instead of switch` |

### Step 0.3 — `VisualBy` duplication
| | |
|---|---|
| **Files** | `VisualBy.java` (99–147) |
| **Change** | Hoist the triplicated `if (context instanceof Driver)` blocks into one shared helper/base |
| **Seam/test** | Existing `VisualSubsystemTest` covers `VisualBy` |
| **Accept** | Single dispatch path; no behaviour change; gate green |
| **Commit** | `De-duplicate VisualBy driver-context dispatch` |

---

## Phase 1 — Extract `VisualFinder` from `Visual` 🟡

**Branch:** `refactor/phase-1-visual-finder` · **Risk:** medium · **Prereq:** none (1.1 before 1.2)

Highest-value God-class win: isolates the OCR find surface and breaks the `Driver ↔ Visual` back-reference cycle, which de-risks Phase 4.

### Step 1.1 — Introduce `OcrEngine` / `ImageMatcher` interfaces (DIP) 🟢
| | |
|---|---|
| **Files** | New `OcrEngine`, `ImageMatcher` interfaces in `visual/`; `OcrService` + `ImageRecognitionService` become default impls |
| **Change** | Extract interfaces in front of the current concrete/static services; keep behaviour identical |
| **Seam/test** | Finder becomes unit-testable with a fake engine (used in 1.2); backend swappable |
| **Accept** | No behaviour change; existing suite green |
| **Commit** | `Introduce OcrEngine and ImageMatcher abstractions` |

### Step 1.2 — Extract `VisualFinder` 🟡
| | |
|---|---|
| **Files** | New `VisualFinder`; move out of `Visual.java`: `findByText/findByImage/findByTextOrImage/findByImageOrText`, their `findAll*` + `findRelative*` variants, `filterAndSelectClosestRelative`, `captureScreenshotBytes`, `verifyOcrEnabled`, retry helpers |
| **Change** | `VisualFinder` depends on the screenshot source + injected `OcrEngine`/`ImageMatcher` (not Applitools fields). `Visual` delegates its public find methods to the finder; `Driver`'s 26 find pass-throughs keep working. *Optional:* move `filterAndSelectClosestRelative`'s `switch(SpatialDirection)` onto `SpatialDirection` itself |
| **Seam/test** | New `VisualFinder` unit tests with a fake `OcrEngine`; re-run `VisualSubsystemTest`, `GeneratedScreenParityTest`, `PlaywrightTs*`, transportnsw |
| **Accept** | `Visual.java` shrinks by the finder surface; find behaviour identical on all engines; `Driver↔Visual` cycle broken |
| **Commit** | `Extract VisualFinder from Visual` |

---

## Phase 2 — `DriverManager` interface + registry 🟡

**Branch:** `refactor/phase-2-driver-manager` · **Risk:** medium · **Prereq:** none (2.1 before 2.2)

### Step 2.1 — Define the `DriverManager` contract 🟡
| | |
|---|---|
| **Files** | New `DriverManager` interface; template = `PlaywrightJavaDriverManager` (already DI-constructed, instance-based). Normalize `AppiumDriverManager` to return `WebDriverSessionResult` (today returns `Driver`) |
| **Change** | Uniform `createSession(...)` / `close(...)` surface; align return shapes so dispatch tables match |
| **Seam/test** | Full suite across all platforms/engines |
| **Accept** | All managers share one contract; no behaviour change |
| **Commit** | `Introduce DriverManager abstraction` |

### Step 2.2 — Replace creation/close switches with a registry 🟡
| | |
|---|---|
| **Files** | `Drivers.createDriverForPlatform` (switch on `Platform`, 112–139), `BrowserDriverManager` (switch on `WebEngine`, 48–72), `Drivers.attachLogsAndCloseDriver` (switch on `driver.getType()`, 279–296) |
| **Change** | `Map<Platform, DriverManager>` / `Map<WebEngine, DriverManager>` registries; move close + log-attach onto the same dispatch |
| **Seam/test** | Full suite; confirm BrowserStack + local paths for each engine; Electron still routes to Selenium |
| **Accept** | No `switch`/`instanceof` on platform/engine for create/close; parity preserved for Selenium / PW-Java / PW-TS / Appium |
| **Commit** | `Dispatch driver creation/close via registry instead of switch` |

---

## Phase 3 — Extract `TeswizConfiguration` from `Setup` 🟡→🔴

**Branch:** `refactor/phase-3-teswiz-config` · **Risk:** medium→high · **Prereq:** none (3.1 before 3.2)

### Step 3.1 — `TeswizConfiguration` holder 🟡
| | |
|---|---|
| **Files** | New `TeswizConfiguration`; `Setup.java` keeps its static facade, delegating to an instance |
| **Change** | Wrap `configs` / `configsBoolean` / `configsInteger` behind safe typed getters (no NPE on missing keys). Callers (`Runner`, managers) untouched this step |
| **Seam/test** | Unit tests for typed getters incl. missing-key defaults |
| **Accept** | Config is injectable; `Setup` facade unchanged externally; gate green |
| **Commit** | `Introduce TeswizConfiguration holder behind Setup` |

### Step 3.2 — Split `Setup`'s responsibilities 🔴 (one commit each)
| | |
|---|---|
| **Files** | Extract from `Setup.java`: `ConfigLoader`, `PlatformTagResolver` (replace `multiuser-*` if/else chain 538–575 with a tag → (Platform, launchName) table), `ApplitoolsConfigFactory`, `CucumberArgsBuilder`, `ReportPortalEnvironmentPublisher` (the `System.setProperty` block) |
| **Change** | Each collaborator instance-based + unit-testable; `Setup` becomes a thin orchestrator. Removes the config class-load ordering trap (managers reading `Runner.getMaxNumberOf*Drivers()` at static-init) |
| **Seam/test** | Unit test per extracted collaborator; full suite after each extraction |
| **Accept** | `Setup.java` materially smaller; no static-init ordering trap; gate green after each commit |
| **Commit** | one per collaborator (see source plan item 9) |

---

## Phase 4 — Decompose `Driver` and `Visual` God classes 🔴

**Branch:** `refactor/phase-4-god-class-decomp` · **Risk:** high (widest blast radius) · **Prereq:** Phases 1, 2, 3 merged

> **No breaking change in 4.1 (revised).** The 26 `Visual` find pass-throughs on `Driver` are **pre-existing stable public API** — they are NOT removed by this phase. The earlier `@Deprecated` marking is being **reverted** (keep them as plain, supported pass-throughs); any hard removal is a separate, pre-announced major-version decision, not part of this refactor. Guiding rule: **no hard removals — deprecate only with explicit maintainer sign-off; default to keeping stable APIs so consumers adopt with zero forced edits.**

### Step 4.1 — Extract from `Driver` 🔴
| | |
|---|---|
| **Files** | New `MobileGestures` (takes `AppiumDriver`): ~28 Appium gesture methods + math helpers + the platform switches `putAppInBackgroundFor` / `pushFileToDevice` / `relaunchApp` / clipboard. New `ElementHighlighter` (web JS): `clearHighlight` / `highlightElement` / `highlightVisualElement` + `activeHighlightBounds` + JS literals. New `ElementFinder` + the `decorateElement` proxy. New **`ElementWaiter`** — owns the bounded synchronisation currently scattered on `Driver` (`waitTillElementIsVisible/Present/Invisible`, `waitForClickabilityOf`, `waitTillVisibilityOfAllElements`, `waitTillPresenceOfAllElements`, `waitForAlert`). **Keep** the 26 pre-existing `Visual` find pass-throughs on `Driver` (revert the `@Deprecated` added earlier — see progress tracker); do NOT delete |
| **Change** | `Driver` becomes a thin facade composing these collaborators; removes `(AppiumDriver)` casts and platform switches. `Driver` keeps thin delegating wait methods. **Additive new capability (the one behaviour-add in this phase, flagged separately):** `ElementWaiter` introduces bounded, non-throwing `boolean isElementVisible(By, int seconds)`, `boolean isElementPresentWithin(By, int seconds)`, `boolean waitTillTextIsPresent(By, String, int seconds)` — closes the "no bounded non-throwing visibility/text probe on any engine" gap. All take `By` (uniform locator currency; Playwright locators via `PlaywrightBy`), so one implementation serves all engines |
| **Change (capability parity, LSP fix)** | Make `waitForAlert` + `getShadowRoot` uniform across engines: today `waitForAlert` is implemented on Selenium/PW-TS but **throws** on PW-Java and is a no-op on Appium; `getShadowRoot` **throws** on PW-Java. Implement on PW-Java (Playwright supports dialogs + shadow DOM natively) **or** surface a single capability-unsupported signal (mirroring the existing `NativeCoordinateInput` pattern) — no divergent per-engine throws |
| **Seam/test** | Full suite (broad recompilation across screen/BL); re-run PW-TS bridge tests. New `ElementWaiterTest` for the additive bounded booleans |
| **Accept** | `Driver.java` is a facade; no `instanceof`/cast ladders; the 26 find pass-throughs remain, un-deprecated; mobile parity N/A (Appium-only gestures), highlight parity N/A (web-only, no-op on Appium) |
| **Commit** | `Extract MobileGestures / ElementFinder / ElementWaiter from Driver (keep find pass-throughs)` + `Add bounded isElementVisible / isElementPresentWithin / waitTillTextIsPresent` + `Make waitForAlert / getShadowRoot uniform across engines` |

### Step 4.2 — Decompose `Visual` behind a `VisualEngine` strategy 🔴 — **DEFERRED**
> **Deferred (revisit on demand).** No benefit to current work and it reshapes the public `Visual` facade — `checkWindow(...)` is the one `Visual` method downstream screens call (e.g. casino web screens), and 4.2 is where the LSP `checkWindow` rework lives. All risk to a consumed API, no near-term payoff. Do only if `Visual` becomes actively painful to change.
| | |
|---|---|
| **Files** | New `PdfVisual`, `WebVisual`, `AppVisual` (PW web folds into `WebVisual`) behind a `VisualEngine` interface; `Visual` becomes a facade composing the one engine for the session |
| **Change** | Removes "all three engines constructed every time" (156–180), the `handleTestResults` driverType switch (~1059), and the LSP bug where a PDF-constructed `Visual` NPEs on `checkWindow` |
| **Seam/test** | Full suite incl. PDF, Applitools-disabled, PW-TS paths |
| **Accept** | Only the session-relevant engine constructed; no driverType switch in result handling; PDF path no longer NPEs |
| **Commit** | `Decompose Visual behind a VisualEngine strategy (PdfVisual / WebVisual / AppVisual)` |

---

## Phase 5 — Unify `browser_config` across engines 🟡→🔴

**Branch:** `refactor/phase-5-browser-config-unify` · **Risk:** medium→high · **Prereq:** independent of 0–4; benefits from Phase 3

> **Split into a non-breaking slice (do now) and a deferred breaking slice.**
> - **DO NOW — Step 5.1 + the resolver wiring in 5.4/5.5, using the EXISTING config keys.** Introduce `WindowSizingResolver` and have Selenium, PW-Java and PW-TS all resolve viewport / maximize / headless sizing through it, so the same `browser_config.json` produces the *same* window on every engine. This is **additive and non-breaking**: no schema change, no key renames, no migration — it only unifies the internal resolution of keys that already exist (`maximize`, `arguments[--window-size]`, `headlessOptions`, `playwright.contextOptions.viewport`). This is the real, user-felt consistency win (today sizing is specified 2–3 times contradictorily and each engine reads a different subset).
> - **DEFERRED — Steps 5.2, 5.3, 5.6 (schema model + rewrite + migration).** These rename keys, rewrite `BrowserConfigSchema.json`, and migrate committed configs — a **user-facing contract change**. Defer to their own release with their own review/announcement. Not needed for the consistency win above.

### Step 5.1 — `WindowSizingResolver` 🟢
| | |
|---|---|
| **Files** | New `config/browser/WindowSizingResolver` |
| **Change** | Input = normalized `window.mode` + runtime viewport (`TESWIZ_DRIVER_VIEWPORT_WIDTH/HEIGHT`); output = `{mode,width,height}`. Single sizing authority for both engines |
| **Seam/test** | New `WindowSizingResolverTest` covering the maximized/fixed/fullscreen × headed/headless matrix |
| **Accept** | No magic numbers; both engines call one resolver |
| **Commit** | `Introduce WindowSizingResolver as single sizing authority` |

### Step 5.2 — `BrowserConfig` model + `BrowserConfigNormalizer` 🟡
| | |
|---|---|
| **Files** | New typed `BrowserConfig` model + `BrowserConfigNormalizer`; `SeleniumDriverManager` + `PlaywrightBrowserConfigResolver` consume the model instead of raw `JSONObject` |
| **Change** | One normalizer feeds both engines (kills pw-java/pw-ts drift); removes strict-`getBoolean` landmines. Normalizer parses unified **and** legacy shapes (for 5.6 in-memory migration) |
| **Seam/test** | New `BrowserConfigNormalizerTest` (first real coverage of Selenium-side config reading) |
| **Accept** | Both engines read the same typed model; legacy still parses |
| **Commit** | `Introduce typed BrowserConfig model and normalizer` |

### Step 5.3 — Rewrite `BrowserConfigSchema.json` 🟡
| | |
|---|---|
| **Files** | `BrowserConfigSchema.json` |
| **Change** | Unified shape; engine-neutral keys required; `additionalProperties:true` confined to `selenium`/`playwright` escape hatches; define `safari` + `playwright` explicitly; fix chrome/firefox `required` asymmetry |
| **Seam/test** | Schema round-trip test — all committed configs validate |
| **Accept** | Schema validates every committed config; no asymmetry |
| **Commit** | `Rewrite BrowserConfigSchema to unified contract` |

### Step 5.4 — Wire the Playwright resolver 🟡
| | |
|---|---|
| **Files** | `PlaywrightBrowserConfigResolver`; verify `PlaywrightWorkerManager.toJson` + `worker.mjs` payload unchanged |
| **Change** | Consume model; replace inline `isMaximized`/viewport block with `WindowSizingResolver`; drop legacy `headlessOptions`/`maximize` reads; align pw-java `buildContextOptions` with pw-ts passthrough |
| **Seam/test** | Rework `PlaywrightBrowserConfigResolverTest` (7 tests assert old shape + hard-coded `1280x960`/`1440x900`); full suite + PW-TS bridge |
| **Accept** | PW sizing via resolver; worker payload shape unchanged; bridge green |
| **Commit** | `Resolve Playwright browser config via unified model` |

### Step 5.5 — Wire `SeleniumDriverManager` 🟡
| | |
|---|---|
| **Files** | `SeleniumDriverManager` |
| **Change** | Consume model; replace `getBoolean(MAXIMIZE)` + the `1920x1080` literal in `manageWindowSizeAndHeadlessMode` with `WindowSizingResolver`; feed `--window-size` for `fixed`; move Chrome prefs/excludeSwitches under the `selenium` hatch; delete dead `addWindowSizeToChromeOptions` from the web path (keep electron) |
| **Seam/test** | Full suite; manual spot-check headed + headless for `maximized` and `fixed` |
| **Accept** | Selenium sizing via resolver; no `1920x1080` literal; electron handling intact |
| **Commit** | `Resolve Selenium browser config via unified model` |

### Step 5.6 — Migration path 🔴 (may be >1 commit)
| | |
|---|---|
| **Files** | Generalize `PlaywrightBrowserConfigMigrator` (legacy → unified); update `PlaywrightBrowserConfigMigrationReporter`; rewrite `configs/browser_config.json`, `configs/second_browser_config.json`, `src/main/resources/default_browser_config.json`; add optional `./gradlew migrateBrowserConfig` task; add `BROWSER_CONFIG_FILE` to `configs/teswiz/teswiz_config.properties.template` |
| **Change** | In-memory compat shim: detect legacy config, migrate in-memory so the run works, print notice, write `<name>-unified.json`. Hard cutover deferred to next release (pre-announced). Preserve each committed file's current behaviour (e.g. `second_browser_config` chrome `maximize:false` → `window.mode:"fixed"`) |
| **Seam/test** | Rework `PlaywrightBrowserConfigMigrationReporterTest` (3 tests on artifact name + messages); full suite; `./gradlew validateConfigurationTemplates` after the template change |
| **Accept** | Legacy configs still run (one release); unified files written; template contains `BROWSER_CONFIG_FILE`; `Changelog.MD` notes the headless-maximized sizing change (now `TESWIZ_DRIVER_VIEWPORT_*` instead of `1920x1080`) |
| **Commit** | `Migrate legacy browser_config to unified contract (with in-memory compat)` + `Rewrite committed browser_config files to unified contract` |

---

## Dependency order

```
Phase 0  (independent, first)
Phase 1  VisualFinder   — 1.1 before 1.2; breaks Driver↔Visual cycle
Phase 2  DriverManager  — independent of Phase 1
Phase 3  TeswizConfig   — 3.1 before 3.2
Phase 4  Driver/Visual decomposition  — LAST; needs 1,2,3
Phase 5  browser_config unify  — independent of 0–4; benefits from 3
```

## Progress tracker

- [x] 0.1 Correctness nits
- [x] 0.2 Provider resolver registries
- [x] 0.3 VisualBy de-duplication
- [x] 1.1 OcrEngine / ImageMatcher abstractions
- [x] 1.2 Extract VisualFinder
- [x] 2.1 DriverManager contract — scoped to web engines: `WebEngineDriverManager` interface
- [x] 2.2 Driver create/close registry — `WebEngine`-keyed registry in `BrowserDriverManager`
- [ ] 2.x (deferred) Normalize `AppiumDriverManager` to return `WebDriverSessionResult`, and collapse the `Platform` create switch + `driver.getType()` close switch in `Drivers`. Deferred: this needs relocating side-effectful `Driver` construction out of `AppiumDriverManager` (device-log capture, session-reuse counters, notifications), a materially wider blast radius than the web-engine dispatch. Best done alongside Phase 4's Driver decomposition.
- [x] 3.1 TeswizConfiguration holder — holder owns the 3 config maps with typed/NPE-safe getters; Setup delegates
- [~] 3.2 Split Setup — in progress (one collaborator per commit):
  - [x] PlatformTagResolver — multi-user tag -> (Platform, launch-name suffix) table replacing the if/else chain
  - [x] ReportPortalEnvironmentPublisher — rpAttributes formatting + ATD/ReportPortal System.setProperty block
  - [x] CucumberArgsBuilder — reporting --plugin args (pretty/html/junit/json/message/timeline) for a log dir
  - [x] ApplitoolsConfigFactory (partial) — extracted ApplitoolsBatchInfoFactory (batch name + BatchInfo build). The remaining initialiseApplitoolsConfiguration machinery (mutable applitoolsConfiguration map + ~12 interdependent helpers, a public static entry point via Runner.getApplitoolsConfiguration) is deferred: a full lift is high-risk for low incremental value and better paired with the Visual engine split in Phase 4.2.
  - [ ] ConfigLoader — DEFERRED (deliberately). buildMapOfRequiredProperties is ~90 lines of `configs.put(KEY, getOverriddenX(KEY, getYFromProperties(KEY, default)))` bound to the `properties` field, the OverriddenVariable helpers, ~40 key constants, and the three maps. Relocating it verbatim is a high-risk, low-value lift against the most central init method every test depends on; it would still just mutate Setup's static maps. The injectable/typed-config value this step targets was already delivered by TeswizConfiguration (3.1), so this extraction is not worth its regression risk on its own.
- [~] 4.1 Extract MobileGestures / ElementFinder / ElementWaiter; KEEP Visual pass-throughs (in progress):
  - [x] **Reverted the `@Deprecated` on the 24 Driver Visual find pass-throughs** (commit 45059d41). Methods kept as plain, supported public API; no deprecation markers remain (verified in Driver.java). In-repo callers still use getVisual() (harmless).
  - [x] **Extract `ElementWaiter`** (commit 45059d41) — `ElementWaiter` in runner package; Driver delegates `isElementVisible(By,int)` / `isElementPresentWithin(By,int)` / `waitTillTextIsPresent(By,String,int)`, each returning false on timeout. Tests: `ElementWaiterTest`.
  - [x] **Capability parity (LSP)** (commit 45059d41) — `WebCapability.SHADOW_DOM`/`ALERTS` added; PW-Java `getShadowRoot()` and `switchTo().alert()` now throw structured `WebEngineCapabilities` diagnostics (verified in PlaywrightJavaWebDriver.java).
  - [x] Extract ElementHighlighter (commit 95f1bfe8) — web JS highlight cluster + activeHighlightBounds; Driver keeps thin delegating methods. Non-breaking.
  - [x] **Universal highlighting across engines** (commit 36537f86, additive — beyond original plan) — transparent `HighlightingPage`/`HighlightingLocator`/`HighlightingFrameLocator` proxies + single `PlaywrightHighlighter`; closed two Selenium highlight bypasses. Tests: `PlaywrightHighlighterTest`, `HighlightingLocatorTest`.
  - [x] Extract MobileGestures (Appium gesture surface) — `MobileGestures` holds the AppiumDriver and owns the W3C touch choreography (scroll/swipe/tap/flick/doubleTap/pinch/zoom/multiTouch) + platform-switch device controls (background/foreground/deepLink/relaunch/clipboard/pushFile/notifications). Driver delegates. Element-resolution-coupled gestures (dragAndDrop, selectNotificationFromNotificationDrawer, horizontalSwipeWithGesture, longPress(By)) intentionally stay on Driver. Non-breaking; full gate green (642 tests).
  - [ ] Extract ElementFinder (By-based findElement/findElements + decorate proxy) — PENDING (still in Driver)
- [ ] 4.2 Visual behind VisualEngine strategy — **DEFERRED** (reshapes consumed `Visual.checkWindow`; no near-term benefit)
- [x] 5.1 WindowSizingResolver (commit 45059d41) — single source of default viewport (`TESWIZ_DRIVER_VIEWPORT_WIDTH/HEIGHT`, default 1280x960). Tests: `WindowSizingResolverTest`.
- [x] 5.4 Wire Playwright resolver to WindowSizingResolver (commit 45059d41) — `PlaywrightBrowserConfigResolver` viewport default resolves from it.
- [x] 5.5 Wire SeleniumDriverManager to WindowSizingResolver (commit 45059d41) — replaced the hard-coded headless `1920x1080` fallback with `WindowSizingResolver.defaultViewport()`.
- [ ] 5.2 BrowserConfig model + normalizer — **DEFERRED** (part of the breaking schema slice)
- [ ] 5.3 Rewrite BrowserConfigSchema — **DEFERRED** (contract change)
- [ ] 5.6 Migration path + committed configs + template — **DEFERRED** (contract change)
- [x] 6.1 Harden `PlaywrightLocator.from(By)` (commit 45059d41) — single prefix->strategy lookup table for standard `By`; `PlaywrightBy`-native locators translate from structured fields. Tests: `PlaywrightLocatorTest`. (Standard Selenium `By` still read via `toString()` by necessity — Selenium exposes no value getter.)
  - [x] 6.2 Unsupported-locator parity — `from(By)` throws a uniform `UnsupportedOperationException` for unknown locators.
  - [ ] 6.3 Document the `By` + `PlaywrightBy` locator contract in the screen-authoring guide — PENDING (class-level Javadoc added in `PlaywrightLocator`; separate authoring-guide doc not yet written)

> Reconciliation note (scan on 2026-10-09): checkboxes above were updated to match the code after commits 45059d41 and 36537f86 landed several plan items (ElementWaiter, capability signals, WindowSizingResolver + wiring, PlaywrightLocator hardening, @Deprecated revert) plus an additive universal-highlighting feature. Version bumped to 1.0.44-SNAPSHOT.

## Out of scope (deliberately)

- Behaviour changes / new features (these are refactors).
- The build-number bump (owned by maintainer; set at release).
- The `log4j-1.2-api` bridge (already removed).
- Rewriting the Applitools/Playwright SDK integration beyond introducing seams.
