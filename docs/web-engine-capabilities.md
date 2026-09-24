# Web Engine Capability & Parity Guide

This document outlines feature parity, capability discovery, and cross-engine support across `selenium`, `playwright-java`, and `playwright-ts` in `teswiz`.

---

## 1. Engine Parity & Capability Support Matrix

| Capability | Selenium | Playwright Java | Playwright TS | Description & Native Alternatives |
| :--- | :---: | :---: | :---: | :--- |
| **Viewport Override** | ✅ | ✅ | ✅ | Configured via `TESWIZ_DRIVER_VIEWPORT_WIDTH` & `TESWIZ_DRIVER_VIEWPORT_HEIGHT`. |
| **Cookie Management** | ✅ | ✅ | ✅ | `addCookie`, `isCookiePresent`, `deleteCookie` via `manage().cookies()` or `BrowserContext.addCookies`. |
| **Stateful Frame Switching** | ✅ | ✅ | ✅ | `switchTo().frame(index)` and `switchTo().frame(nameOrId)`. In Playwright Java, native `FrameLocator` is also recommended. |
| **Window Resizing** | ✅ | ✅ | ✅ | `manage().window().setSize(...)` / `setViewportSize(...)`. |
| **Async Script Execution** | ✅ | ✅ | ✅ | `executeAsyncScript` with Promise completion and timeout support. |
| **Network Interception** | ✅ | ✅ | ✅ | Route handling and request interception. |

---

## 2. Fail-Fast & Diagnostic Messages

`teswiz` enforces a **fail-fast strategy** to prevent silent test failures or lossy behavior when using non-Selenium web engines.

When an unsupported capability or invalid operation is invoked:
1. **Pre-flight Check**: Validated during driver instantiation or Screen contract initialization.
2. **Runtime Protection**: Throws a structured `UnsupportedOperationException` detailing the exact reason, native alternatives, and resolution steps.

### Diagnostic Message Example:

```text
================================================================================
[TESWIZ CAPABILITY ERROR] Unsupported Web Capability Requested!
================================================================================
Capability : FRAMES (switchTo().frame)
Web Engine : WEB_ENGINE=playwright-java

REASON:
  The active engine 'playwright-java' does not support traditional Selenium 
  stateful frame switching via WebDriver.switchTo().frame().

HOW TO RESOLVE:
  Option 1 (Recommended): Use native Playwright Java FrameLocator:
            context.page().frameLocator("iframe#my-frame").locator("button")
  Option 2: Switch to an engine with full frame compatibility:
            Set WEB_ENGINE=playwright-ts or WEB_ENGINE=selenium in properties.

DOCUMENTATION:
  https://github.com/znsio/teswiz/blob/main/docs/web-engine-capabilities.md
================================================================================
```

---

## 3. Recommended Architectural Patterns

When authoring web tests in `teswiz`:

1. **Step Definitions (`*Steps.java`)**:
   - Must only call Business Layer methods.
   - Do NOT call driver/manage APIs directly.

2. **Business Layer (`*BL.java`)**:
   - Manages assertion logic (`SoftAssertions`) and flow coordination.
   - Calls Screen contract methods (`Screen.get()`).

3. **Screen Contracts (`*Screen.java`) & Implementations**:
   - Abstract contract in `com.znsio.teswiz.screen.<domain>`.
   - Selenium implementation in `com.znsio.teswiz.screen.web.<domain>.*ScreenWeb`.
   - Playwright Java implementation in `com.znsio.teswiz.screen.web.playwrightjava.<domain>.*ScreenPlaywrightJava`.
   - Playwright TS implementation in `src/test/resources/playwright/screens/<domain>/*-screen.ts`.
