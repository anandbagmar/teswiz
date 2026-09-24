# Web-engine parity and capability discovery in teswiz

**Status:** proposal for discussion
**Audience:** teswiz maintainers
**Raised by:** Casino Integration Test (CIT) team, from adopting `WEB_ENGINE=playwright-java`
**teswiz version examined:** 1.0.37 (Playwright 1.63.0)

---

## TL;DR

teswiz ships three web engines — `selenium`, `playwright-java`, `playwright-ts` — behind one
Selenium-shaped `WebDriver` facade. The two Playwright engines are **not at parity**:
`playwright-java` throws `unsupported` for frames, cookies, window sizing and async scripts,
while `playwright-ts` implements all of them through its worker protocol.

Nothing here is a Playwright limitation. Every gap has a direct API counterpart, and for most of
them **a working reference implementation already exists inside teswiz** — in the TS worker. The
`playwright-java` in-process adapter simply lags behind it.

Three things are proposed:

1. **Close the `playwright-java` gaps**, using the TS worker's behaviour as the specification.
2. **Make capabilities declarable and queryable**, so gaps surface early with an actionable
   message instead of mid-scenario as `unsupported`.
3. **Add a cross-engine conformance suite**, so "one spec, three engines" is enforced rather than
   hoped for.

---

## How this surfaced

We adopted `playwright-java` for browser tests. The switch itself was a one-line config change
and worked immediately — the adapter implements enough of `WebDriver` that our existing
Selenium-shaped Screens ran unchanged, drove a full login flow on a live site, and produced
trace/HAR/console artefacts.

The problem was **discovering what would not work**. We learned that `switchTo().frame(...)`
throws only by decompiling the published jar and reading the adapter — after which we could see
that one of our Screens called it on a code path masked by a placeholder locator. Had that
locator been real, the failure would have appeared inside a scenario, in CI, as
`unsupported: frame(WebElement)`, with no indication that another engine supports it or what to
use instead.

That discovery cost is the actual problem. The missing methods are its symptom.

---

## Evidence: current parity

Verified by reading the 1.0.37 sources. `playwright-java` =
`web/playwright/PlaywrightJavaWebDriver` (in-process); `playwright-ts` =
`web/playwright/PlaywrightWebDriver` (worker-backed, commands in `PlaywrightWorkerClient`).

| `WebDriver` capability | selenium | playwright-java | playwright-ts |
|---|---|---|---|
| `switchTo().frame(int / String)` | yes | **throws** `unsupported` | yes — `switchToFrame` |
| `switchTo().frame(WebElement)` | yes | **throws** `unsupported` | yes — via locator reference |
| `switchTo().parentFrame()` / `defaultContent()` | yes | **throws** `unsupported` | yes — `switchToParentFrame` |
| `manage().window().setSize` / `getSize` | yes | **throws** `unsupported` | yes — `setWindowSize` |
| `manage().window().setPosition` | yes | **throws** `unsupported` | yes — `setWindowPosition` |
| `manage().addCookie` / `deleteCookieNamed` / `deleteAllCookies` / `getCookies` | yes | **throws** `unsupported` | yes — worker cookie commands |
| `executeAsyncScript` | yes | **throws** `unsupported` | **silently wrong** — delegates to `executeScript`, ignoring the callback contract |
| `switchTo().window` / `newWindow` | yes | **throws** `unsupported` | yes |
| Context options from `browser_config.json` | n/a | **lossy** — only `ignoreHTTPSErrors` + `baseURL` honoured | needs the same audit |

Two observations matter more than the individual cells:

- **`playwright-java` is the outlier, not Playwright.** The TS worker proves each capability is
  reachable; the Java adapter just hasn't implemented it.
- **Failure modes are inconsistent.** `playwright-java` throws — honest, but late.
  `playwright-ts`'s `executeAsyncScript` returns a wrong answer — silent, and possibly never
  noticed. For a framework whose job is catching false greens, the silent one is worse.

### The lossy config mapping

`PlaywrightBrowserConfigResolver` already forwards the **entire** `playwright.contextOptions` map
from `browser_config.json`. `PlaywrightJavaDriverManager.buildContextOptions` then consumes three
of roughly twenty-five available options:

```java
Browser.NewContextOptions options = new Browser.NewContextOptions();
if (contextOptions.containsKey("ignoreHTTPSErrors")) { ... }
if (contextOptions.containsKey("baseURL")) { ... }
options.setRecordHarPath(harFile);
return options;
```

Consequence for us: the browser is pinned at Playwright's default **1280x720 viewport with no way
to override it** — `viewport` in `contextOptions` is silently ignored, and
`manage().window().setSize` throws. A consumer authoring responsive-layout locators has no lever.

The silence is the sharper edge: someone setting `viewport`, `locale`, `userAgent`,
`deviceScaleFactor`, `permissions`, `geolocation`, `storageState`, `recordVideo`,
`httpCredentials` or `extraHTTPHeaders` gets no error and no effect.

Worth noting that teswiz **already defines** `TESWIZ_DRIVER_VIEWPORT_WIDTH/HEIGHT` runtime
config — but they only feed Applitools' viewport (`Setup.getViewportSize()`), never the browser
context.

---

## Design principles for a three-engine framework

1. **One behavioural contract, many implementations.** If a capability is in the facade, either
   every engine implements it with the same observable semantics, or the facade says out loud that
   this engine does not have it. No third state.
2. **Fail at the earliest possible moment.** Startup beats scenario time. A run that has already
   leased an account and opened a browser is the worst place to learn the engine lacks frames.
3. **Never silently do something different.** See `executeAsyncScript` on playwright-ts.
4. **Capabilities are data, not scattered conditionals.** One declaration, consumed by the
   adapters, the screen resolver, the reporter and the docs.
5. **Config is engine-neutral at the edge, engine-specific at the boundary.** A consumer should
   express "viewport 1600x900" once; each engine maps it or declares it unsupported.
6. **The native API is a feature, not a leak.** The facade is a migration bridge;
   `PlaywrightJavaScreenContext` exposing `page`/`browserContext` is what makes Playwright worth
   choosing. Say so explicitly.

---

## Proposed design

### D1. An engine capability registry

One enum of capabilities, plus a per-engine declaration:

```java
public enum WebCapability {
    FRAMES, WINDOW_RESIZE, WINDOW_POSITION, COOKIE_MANAGEMENT,
    ASYNC_SCRIPT, MULTIPLE_WINDOWS, NETWORK_INTERCEPTION, VIEWPORT_OVERRIDE
}

public interface WebEngineCapabilities {
    boolean supports(WebCapability capability);
    Optional<String> alternativeFor(WebCapability capability);   // what to use instead
}
```

Every `unsupported(...)` site routes through one helper that produces something actionable rather
than a bare method name:

```
FRAMES is not available on WEB_ENGINE=playwright-java.
Use context.page().frameLocator(selector) from PlaywrightJavaScreenContext,
or run with WEB_ENGINE=playwright-ts / selenium.
See <docs link>.
```

The value goes beyond better messages: consumers can **assert** against it. Our project already
has an architecture test pinning Screen structure; with a queryable capability model we could
also fail our build when a Screen uses a capability the configured engine lacks — seconds instead
of a CI scenario.

This is also the natural source for a **published support matrix**, generated from the same
declaration so it cannot drift from the code.

### D2. Engine-neutral context options, mapped per engine

A small typed model of what a browser context can be given, independent of engine:

```java
public record WebContextOptions(
        Optional<Dimension> viewport,        // empty = engine default; explicit null = full window
        Optional<String> locale,
        Optional<String> timezoneId,
        Optional<String> userAgent,
        Optional<Double> deviceScaleFactor,
        boolean ignoreHttpsErrors,
        Map<String, String> extraHttpHeaders,
        ... ) { }
```

- **playwright-java** — map onto `Browser.NewContextOptions`; this deletes the lossy method.
- **playwright-ts** — serialise onto the existing `createSession` `browserConfig` payload, which
  the worker already receives.
- **selenium** — map what has an equivalent (`window().setSize` for viewport, `--lang` for locale,
  `acceptInsecureCerts`) and **declare the rest unsupported** via D1 rather than ignoring it.

Defaults come from the existing `TESWIZ_DRIVER_VIEWPORT_*` config, so one setting drives both the
browser context and the Applitools viewport instead of only the latter.

Any option an engine cannot honour should emit a **startup warning naming the option and the
engine**. Silent dropping is the current defect and the easiest to fix.

### D3. A search-root abstraction for playwright-java frames

The Java adapter's blocker is a single hardcoded root, repeated at ~16 call sites:

```java
public WebElement findElement(By by) {
    Locator locator = session.page().locator(PlaywrightJavaBy.toSelector(by));   // always the page
```

Selenium's frame model is stateful; Playwright's is not. Reconcile them by making the root
explicit and swappable:

```java
interface PlaywrightSearchRoot {
    Locator locator(String selector);
    Object evaluate(String expression, Object arg);
}
// implementations over Page, Frame and FrameLocator

private PlaywrightSearchRoot searchRoot = SearchRoots.of(session.page());
```

`switchTo()` then becomes a root swap, and every piece already exists in Playwright 1.63:

| Selenium | Playwright 1.63 |
|---|---|
| `frame(WebElement)` | `locator.contentFrame()` → `FrameLocator`, or `elementHandle.contentFrame()` → `Frame` |
| `frame(String nameOrId)` | `page.frame(name)`, else `page.frameLocator("iframe[name=…], iframe#…")` |
| `frame(int index)` | `page.frames()` — **note** `frames().get(0)` is the main frame, so Selenium's index needs `+1` |
| `parentFrame()` | `frame.parentFrame()` |
| `defaultContent()` | reset the root to `page` |

The state is per-driver, and a driver is already per-persona/per-session, so nothing is shared
across scenarios. The index offset is the one semantic worth a dedicated test.

Cookies and async script need no new abstraction. `BrowserContext.addCookies/cookies/clearCookies`
map directly; `executeAsyncScript` is a Promise wrapper in which Selenium's trailing callback
becomes `resolve`:

```java
String expression = "(args) => new Promise((resolve) => { const callback = resolve; %s })"
        .formatted(adapt(script));   // arguments[arguments.length - 1] → callback
```

**The same wrapper fixes the playwright-ts silent-wrong-answer defect**, which today forwards to
`executeScript`.

### D4. A cross-engine conformance suite

The parity table above should be a test, not a document. One behavioural spec, executed three
times — once per engine — against a local fixture page:

```
webdriver-conformance/
  FramesConformance      enter iframe, read text, parentFrame, defaultContent
  CookieConformance      add, read, delete named, delete all
  ViewportConformance    request 1600x900, assert window.innerWidth/innerHeight
  ScriptConformance      sync result, async callback result, element argument
```

Each case either passes or is **skipped because the engine declares the capability unsupported**
(D1) — never silently absent. A new engine, or a regression in an existing one, then shows up in
teswiz's CI rather than in a consumer's project months later.

It is also how the Java adapter should be brought up: take the TS worker as the reference, write
the conformance test, watch `playwright-java` fail, implement.

### D5. Position the native API explicitly

The Selenium facade makes `playwright-java` feel like a drop-in — excellent for migration,
misleading as a destination. Auto-waiting, `frameLocator`, `route()` interception, web-first
assertions and tracing are all invisible through it. We rewrote our four Screens onto
`PlaywrightJavaScreenContext` (`page`, `browserContext`) and deleted 16 hand-rolled sleeps plus a
`JavascriptExecutor` workaround in the process.

Suggested framing for teswiz's docs: **the facade is for migrating existing Selenium Screens; the
engine-native screen context is for new work.** Then invest in that native surface — `route()`
mocking helpers, trace/HAR accessors, `expect()`-style assertions wired into teswiz's
soft-assertion collector — rather than in facade completeness alone.

---

## Impact per engine

| Change | selenium | playwright-java | playwright-ts |
|---|---|---|---|
| D1 capability registry | declares full support | declares gaps honestly, better messages | declares support; `ASYNC_SCRIPT` flagged until the wrapper lands |
| D2 context options | maps viewport/locale, declares the rest | **removes the lossy mapping** — the main win | audit + serialise onto `createSession` |
| D3 frames / cookies / async / window | no change | **implements the gaps** | reuses the async wrapper |
| D4 conformance suite | baseline reference | proves the new implementations | catches the silent-wrong-answer class |
| D5 native-first docs | n/a | `PlaywrightJavaScreenContext` | TS screen modules |

---

## Suggested sequencing

| Phase | Work | Why this order |
|---|---|---|
| 1 | D2 for playwright-java — context options + viewport, defaulting from `TESWIZ_DRIVER_VIEWPORT_*` | smallest diff, unblocks real consumer work today, no behaviour change for anyone on defaults |
| 2 | D1 capability registry + actionable `unsupported` messages | stops the next team decompiling jars; independent of any implementation work |
| 3 | D4 conformance suite, seeded from the parity table | makes phase 4 test-first and prevents regressions |
| 4 | D3 frames, cookies, window sizing, async script in playwright-java; async fix in playwright-ts | largest, and safest once phase 3 exists |
| 5 | D5 docs — generated support matrix + native-first guidance | closes the discovery gap this proposal is really about |

Phases 1 and 2 are independently shippable and would have prevented most of our lost time on
their own.

---

## Open questions for maintainers

1. Is facade completeness a goal, or is the Selenium interface explicitly a migration bridge? The
   answer decides whether D3 is a priority or a low-value backfill.
2. Should an engine-unsupported capability **throw at the call** or **fail the run at startup**
   when a Screen is known to use it? We would prefer startup.
3. For D2, is a typed engine-neutral options model welcome, or would you rather keep raw
   per-engine maps and consume more keys? The typed model costs more, but it is what lets
   `selenium` declare gaps instead of ignoring them.
4. Does playwright-ts intend to keep the worker protocol as its long-term transport? If so, the
   conformance suite needs a worker fixture in CI.
5. Would you accept these as separate PRs, in the sequence above?

---

## Appendix: how this was verified

No behaviour was inferred from documentation or release notes. Findings come from reading the
1.0.37 sources jar (`teswiz-1.0.37-sources.jar`) and the resolved `playwright-1.63.0` jar:

- `web/playwright/PlaywrightJavaWebDriver` — the `unsupported(...)` call sites, and `findElement`'s
  hardcoded `session.page()` root
- `web/playwright/PlaywrightWebDriver` + `PlaywrightWorkerClient` — the TS worker's
  `switchToFrame`, `switchToParentFrame`, `setWindowSize`, `setWindowPosition`, cookie commands,
  and `executeAsyncScript` delegating to `executeScript`
- `web/playwright/PlaywrightJavaDriverManager.buildContextOptions` — the three consumed options
- `config/browser/PlaywrightBrowserConfigResolver` — confirms the full options map is already
  forwarded to that method
- `runner/Setup.getViewportSize` + `config/TeswizRuntimeConfiguration` — `DRIVER_VIEWPORT_*` exists
  and is Applitools-only
- `javap` against `playwright-1.63.0` — `ElementHandle.contentFrame()`, `Locator.contentFrame()`,
  `Page.frame/frames/mainFrame/setViewportSize`,
  `BrowserContext.addCookies/cookies/clearCookies`

Also confirmed for the record: `PlaywrightJavaWebDriver` is byte-identical between 1.0.36 and
1.0.37, so none of this changed in that release.

**Not verified:** we have not run teswiz's own test suite, and the conformance suite in D4 is a
proposal, not something we have prototyped.
