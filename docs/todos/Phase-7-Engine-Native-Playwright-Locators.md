# teswiz — Phase 7: engine-native Playwright locators (remove the By bridge)

> Status: **Not started** — captured for pickup. Decisions below are locked with the maintainer.
> Branch: continue on `refactor/phase-4-god-class-decomp` (already pushed to origin; GitHub Actions
> is the regression gate). No separate branch.

## Context & relationship to the SOLID plan

The SOLID/OOP refactoring plan is complete except for items the maintainer has **deliberately
skipped**: 2.x (Appium DriverManager normalization), 3.2 remainder (ConfigLoader + full
ApplitoolsConfigFactory), 4.2 (Visual→VisualEngine), 5.2/5.3/5.6 (unified browser_config schema +
migration), and 6.3 (document By/PlaywrightBy — **not required**, superseded by this phase). Those
are marked Skipped in the SOLID plan docs. None are prerequisites for Phase 7. Phase 7 is the only
remaining active teswiz work.

## Principle

- **Selenium + Appium keep `By`.** It is their genuine native locator; all current users are on it.
  **Zero changes, zero break — this is a hard guarantee.**
- **Playwright is authored natively** — pw-java via `context.page()` (Java `Page`/`Locator`), pw-ts
  via its `.screen.ts` TypeScript modules. Both Screen layers are *already* native today.
- **Delete the Playwright↔`By` translation bridge.** Safe: Playwright has **no external users**, and
  the maintainer has explicitly waived pw-java/pw-ts backward compatibility.
- **Multi-user/multi-platform preserved.** Personas may mix (e.g. one Playwright-web + one Appium);
  `WEB_ENGINE` is global per run, so all web personas already share one engine. No change.

## Non-negotiable guardrail

**Before deleting any shared class, verify and report Selenium/Appium impact. If removing the
Playwright bridge would change ANY Selenium or Appium behaviour or public API, STOP and surface it
to the maintainer first.** The shared surfaces to scrutinise: `ElementWaiter`, `Driver`'s `By`-typed
methods, `ElementFinder`, and anything typed on `WebElement`/`By` that both Playwright and
Selenium/Appium flow through.

## What gets deleted (all Playwright-only, confirmed translation-layer)

- `web/playwright/PlaywrightBy.java` (public By-factory for Playwright strategies) + migrate its one
  in-repo user (`TransportNswOcrScreenPlaywrightJava` test screen) to native `context.page()`.
- `web/playwright/PlaywrightJavaBy.java` (By → Playwright selector string).
- `web/playwright/PlaywrightLocator.java` + `web/playwright/PlaywrightLocatorReference.java`
  (By → `{strategy,value,index,parent}` JSON for the pw-ts worker).
- The pw-java `findElement(By)` / `findElements(By)` translation path in `PlaywrightJavaWebDriver` /
  `PlaywrightJavaWebElement`.
- The pw-ts worker locator protocol in `PlaywrightWorkerClient` + `worker.mjs createLocator` (reduce
  to only what the `.screen.ts` screen-action path needs — which does NOT use `By`).
- The Playwright `WebElement` impls (`PlaywrightJavaWebElement`, `PlaywrightWebElement`) **iff** no
  surviving internal service needs them (see "keep-alive set").

## The keep-alive set — what must be resolved before deletion compiles

These are the only things that drag `By` into the Playwright engines today:
1. **`ElementWaiter`** — Selenium `WebDriverWait`/`ExpectedConditions` over `By`; on Playwright it
   resolves via `findElement(By)`. Must keep working for Se/Appium; on Playwright it needs a native
   replacement (Playwright `Locator.waitFor` + auto-wait) **or** a capability-unsupported signal.
2. **`Driver`'s `By` helpers** (`findElement(By)`, `isElementPresent(By)`, `waitForClickabilityOf`,
   `waitTill*`) — keep for Se/Appium; on Playwright, native or unsupported-signal.
3. **pw-ts worker element-command protocol** — the `.screen.ts` path does not use it; the generic
   facade does. Reduce/remove alongside the facade decision.

Confirmed NOT in the keep-alive set (safe, independent): Visual/OCR (driver-level `getScreenshotAs`),
highlighting (self-contained on native `Locator`), `ElementFinder` (returns Playwright elements
un-decorated).

## Staged sequence (each a commit; gate = clean test green + validateConfigurationTemplates + CI)

- **7.1 — Verify Se/Appium isolation (NO code).** Prove `ElementWaiter` + `Driver` `By`-methods +
  `ElementFinder` have no Playwright-only coupling whose removal changes Se/Appium. Write the finding
  down. If any Se/Appium change is implied → STOP, report, revise plan. **Gate for everything after.**
- **7.2 — Native Playwright waits.** Add `Locator`-based bounded waits so removing `By`-waits on
  Playwright loses no capability. Keep `ElementWaiter`/`By` waits for Se/Appium untouched. Tests added.
- **7.3 — Stop routing Playwright through the `By` facade.** `findElement(By)`/`By`-waits on the
  Playwright engines either delegate to native internally or throw a structured
  `WebEngineCapabilities` unsupported signal (mirror `NativeCoordinateInput`). Se/Appium unchanged.
- **7.4 — Migrate the one `PlaywrightBy` user** (`TransportNswOcrScreenPlaywrightJava`) to native
  `context.page()`; remove any remaining internal `PlaywrightBy`/`By`-on-Playwright references.
- **7.5 — Delete the bridge.** Remove `PlaywrightBy`, `PlaywrightJavaBy`, `PlaywrightLocator`,
  `PlaywrightLocatorReference`, the pw-java `findElement(By)` impl, the pw-ts worker locator protocol,
  and the Playwright `WebElement` impls if now unused. Delete/rewrite `PlaywrightLocatorTest`; adjust
  worker-protocol tests. `worker.mjs` keeps only the `.screen.ts` screen-action path.
- **7.6 — Docs + Changelog.** Document native authoring as THE Playwright model (pw-java
  `context.page()`, pw-ts `.screen.ts`); note `By` is Selenium/Appium-only. `Changelog.MD` entry.

## Test impact (from investigation)

- Survive as-is: `PlaywrightHighlighterTest`, `HighlightingLocatorTest` (native-`Locator` based).
- Delete/rewrite at 7.5: `PlaywrightLocatorTest`; adjust `PlaywrightWorkerClient*` protocol tests.
- Watch (parity): `GeneratedScreenParityTest`, `PlaywrightTsScreenBridgeFactoryTest`,
  `ScreenContractSanityCheckerTest`.
- Note: no `PlaywrightJavaByTest` and no `ScreenPatternArchitectureTest` exist (do not assume them).

## Parity / blast radius

- **Se/Appium: zero change** in every step (hard guarantee; 7.1 proves it before anything is removed).
- **pw-java: no Screen change** — already native; highlighting already transparent (done earlier).
- **pw-ts: no Screen change** — locators already live in `.screen.ts`.
- The only behaviour change is `By`-on-Playwright becoming native-or-unsupported — hits Playwright
  only, which has no external users.

## Decisions locked

1. **Delete the bridge** (not keep-deprecated). No pw-java/pw-ts back-compat. Se/Appium guardrail above.
2. **No Java-side pw-ts native handle** (7-ts-B rejected) — pw-ts stays native-in-TypeScript.
3. **Same branch** `refactor/phase-4-god-class-decomp` (pushed); CI is the net.
4. **teswiz version / consumption:** stays `1.0.44-SNAPSHOT`, consumed by casino via `mavenLocal()`
   (local `publishToMavenLocal`). **JitPack cannot serve the branch-SNAPSHOT** (verified: branch build
   yields `modules: []` / "Not found"). For remote/CI consumption, cut a **Git tag** (e.g. `1.0.44`)
   — a tag builds once on JitPack and is served reliably; then consumers use
   `com.github.anandbagmar:teswiz:1.0.44`.
