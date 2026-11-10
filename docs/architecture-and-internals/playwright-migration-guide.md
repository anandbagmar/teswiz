# Playwright Migration & Implementation Guide
[📚 Documentation Index](../index.md) • [🏠 Main README](../../README.md)

---


## Table of Contents

- [1. Selecting the Web Engine](#1-selecting-the-web-engine)
- [2. Defining the Screen Contract](#2-defining-the-screen-contract)
- [3. Implementing for Selenium Web](#3-implementing-for-selenium-web)
- [4. Implementing for Playwright-Java Web](#4-implementing-for-playwright-java-web)
- [5. Implementing for Playwright-TS Web (Zero-Java Proxying)](#5-implementing-for-playwright-ts-web-zero-java-proxying)
- [6. Verifying Screen Parity and Compliance](#6-verifying-screen-parity-and-compliance)
- [7. Migrating API Execution from RestAssured to Playwright-Java (`API_ENGINE`)](#7-migrating-api-execution-from-restassured-to-playwright-java-api_engine)
  - [Option A: Standard Migration (Using `ApiService` Facade)](#option-a-standard-migration-using-apiservice-facade)
  - [Option B: Porting Legacy Direct RestAssured Calls](#option-b-porting-legacy-direct-restassured-calls)

---


This guide describes how to implement web screens in teswiz using Selenium, Playwright-Java, and Playwright-TS while keeping the shared screen-contract pattern.

---

## 1. Selecting the Web Engine

You can configure the active web execution engine on a per-suite basis by setting the `WEB_ENGINE` property in your configuration properties file (e.g., `ajio_local_web_config.properties`):

```properties
# Valid options: selenium | playwright-java | playwright-ts
WEB_ENGINE=playwright-ts
```

If not specified, Teswiz defaults to `selenium`.

---

## 2. Defining the Screen Contract

All platform and engine implementations must implement a single shared Java contract. `ScreenRegistry`
resolves implementation classes (and Playwright-TS modules) by convention from the **contract class's own
package** — it does not require that package to be `com.znsio.teswiz.screen`. The only rule is that the
contract's package must contain a segment literally named `screen`; everything before that segment is your
project's own namespace, and everything after it (if anything) is treated as the "domain" (e.g. `ajio`) that
gets reused for every platform/engine implementation package.

For example, in your own project the contract might live under
`src/test/java/com/acme/tests/screen/ajio/`:

```java
package com.acme.tests.screen.ajio;

public abstract class HomeScreen {
    public static HomeScreen get() {
        return com.znsio.teswiz.screen.ScreenRegistry.getScreen(HomeScreen.class);
    }

    public abstract SearchScreen searchForTheProduct(String productName);
}
```

(`com.znsio.teswiz.screen.ScreenRegistry` itself is always the framework class from the teswiz JAR — only the
*contract's* package is up to you.)

---

## 3. Implementing for Selenium Web

The Selenium Web implementation resides under `<contract-root-package>/web/<app_name>/` — i.e. sibling to the
contract's own package, under a `web` segment. The class name must append `Web` to the contract class name:

```java
package com.acme.tests.screen.web.ajio;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.acme.tests.screen.ajio.HomeScreen;
import com.acme.tests.screen.ajio.SearchScreen;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

public class HomeScreenWeb extends HomeScreen {
    private final Driver driver;
    private final Visual visually;
    private static final By SEARCH_INPUT = By.name("searchVal");

    public HomeScreenWeb(Driver driver, Visual visually) {
        this.driver = driver;
        this.visually = visually;
    }

    @Override
    public SearchScreen searchForTheProduct(String productName) {
        driver.waitTillElementIsPresent(SEARCH_INPUT);
        driver.findElement(SEARCH_INPUT).sendKeys(productName, Keys.ENTER);
        return SearchScreen.get();
    }
}
```

---

## 4. Implementing for Playwright-Java Web

The Playwright-Java Web implementation resides under `<contract-root-package>/web/playwrightjava/<app_name>/`.
The class name must append `PlaywrightJava` to the contract class name:

```java
package com.acme.tests.screen.web.playwrightjava.ajio;

import com.microsoft.playwright.Locator;
import com.acme.tests.screen.ajio.HomeScreen;
import com.acme.tests.screen.ajio.SearchScreen;
import com.znsio.teswiz.web.playwright.screen.PlaywrightJavaScreenContext;

public class HomeScreenPlaywrightJava extends HomeScreen {
    private final Locator searchInput;

    public HomeScreenPlaywrightJava(PlaywrightJavaScreenContext context) {
        this.searchInput = context.page().locator("input[name='searchVal']");
    }

    @Override
    public SearchScreen searchForTheProduct(String productName) {
        searchInput.fill(productName);
        searchInput.press("Enter");
        return SearchScreen.get();
    }
}
```

Important:

- `playwright-java` screens should be authored with native Playwright Java APIs
- use `Page`, `Locator`, and Playwright-native actions/waits inside the screen implementation
- do not treat Selenium `By` locators as the intended long-term authoring model for `playwright-java`

---

## 5. Implementing for Playwright-TS Web (Zero-Java Proxying)

When using `playwright-ts`, **you do not write any Java implementation class**. The framework uses ByteBuddy at runtime to dynamically subclass your Java contract (`HomeScreen.java`) and intercept all calls, proxying them over IPC to a node worker running your TypeScript screen module.

The TypeScript module resides under `src/test/resources/playwright/screens/<app_name>/` (this path is fixed,
regardless of your contract's Java package) and the file name must be `kebab-case.screen.ts` (matching the
`CamelCase` contract name, e.g. `home.screen.ts`):

```typescript
import type { ScreenContext } from "../screen-context.ts";

const LOCATORS = {
  searchInput: 'input[name="searchVal"]',
};

export async function searchForTheProduct(screen: ScreenContext, productName: string): Promise<void> {
  await screen.page.locator(LOCATORS.searchInput).fill(productName);
  await screen.page.locator(LOCATORS.searchInput).press("Enter");
}
```

---

## 🧪 6. Verifying Screen Parity and Compliance

The `verifyScreenContracts` Gradle task checks screen contracts for missing implementation classes, missing
TypeScript modules, or method mismatches. Teswiz's own `build.gradle` runs it against
`src/test/java/com/znsio/teswiz/screen`.

`build.gradle.sample` ships the same task for your project. Point it at your own screen source root in
`gradle.properties`, then run `./gradlew verifyScreenContracts`:

```properties
teswizScreenContractsRoot=src/test/java/com/acme/tests/screen
```

## 6a. Running Playwright-TS outside the teswiz repository

The playwright-ts engine drives the browser through a Node worker (`playwright/worker.mjs`). The teswiz jar
bundles it. When your project has no `playwright/worker.mjs` of its own, teswiz extracts the bundled worker to
`build/teswiz/playwright/` in your project at run time. A project copy of `playwright/worker.mjs`, if present, is
used instead.

The worker imports its npm packages from your project, so install them once in the project root. Pin npm
`playwright` to the same minor version as the Playwright Java (`com.microsoft.playwright:playwright`) that your
teswiz version ships (currently 1.64; see `com.microsoft.playwright:playwright` in teswiz's `build.gradle`):

```bash
npm install --save-exact playwright@1.64 @applitools/eyes-playwright
npx playwright install
```

> [!IMPORTANT]
> Keep npm `playwright` and Playwright Java on the same minor version. Each Playwright minor uses its own
> browser build, and both share one browser cache (`~/Library/Caches/ms-playwright` on macOS,
> `~/.cache/ms-playwright` on Linux). With mismatched minors, each `playwright install` removes the other's
> browsers, so `playwright-java` (or `playwright-ts`) runs fail with `browserType.launch: Executable doesn't
> exist`. When you upgrade teswiz, check its Playwright Java version and move npm `playwright` with it.

The worker runs with `node --experimental-strip-types`, which needs Node.js 22.6 or newer (22.12+ or 24+ when the
project also installs Appium 3, see [Prerequisites](../getting-started/prerequisites.md)). If the worker exits
early (for example because a package is missing), the error now includes the worker's stderr.

---

## 7. Migrating API Execution from RestAssured to Playwright-Java (`API_ENGINE`)

### Option A: Standard Migration (Using `ApiService` Facade)

If your test codebase already uses the engine-agnostic `ApiService` facade (`ApiService.get()`, `ApiService.post()`, etc.), **zero code changes are needed**.

Simply change `API_ENGINE` in your suite configuration or environment variables:

```properties
# Change from rest-assured (default) to playwright-java
API_ENGINE=playwright-java
```

Or via command line execution:

```bash
API_ENGINE=playwright-java CONFIG=./configs/api_local_config.properties TAG=@api PLATFORM=api ./gradlew run
```

### Option B: Porting Legacy Direct RestAssured Calls

If legacy test code calls `io.restassured.RestAssured` directly in step definitions or business layers:

1. **Replace RestAssured invocations with `ApiService`**:
   - `RestAssured.given().headers(...).get(url)` $\rightarrow$ `ApiService.get(url, queryParams, headers)`
   - `RestAssured.given().body(jsonMap).post(url)` $\rightarrow$ `ApiService.post(url, jsonMap, headers)`

2. **Update response assertions to `TeswizApiResponse`**:
   - `response.getStatusCode()` $\rightarrow$ `response.getStatusCode()`
   - `response.getBody().asString()` $\rightarrow$ `response.getResponseText()` or `response.asJsonObject()`
   - `response.getTime()` $\rightarrow$ `response.getResponseTimeInMs()`
   - `response.getHeaders()` $\rightarrow$ `response.getHeaders()`

3. **Set `API_ENGINE=playwright-java` in suite configuration.**

> **The request is the same on every engine.** A `Map` / `Collection` body is written by one shared Jackson serialiser, so explicit `null` fields and list elements are sent (not dropped) and a decimal such as `1.0` stays `1.0`. A URL you pass is treated as already encoded, so a valid `%XX` escape is never encoded a second time, and `queryParams` values are encoded once. If a test depended on a `null` field being omitted, remove the key from the map instead. See [What goes on the wire](../engines/running-api-tests.md#what-goes-on-the-wire-same-for-every-engine).
