package com.znsio.teswiz.web.playwright;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.znsio.teswiz.config.browser.PlaywrightBrowserConfig;

/**
 * Test-only factory that launches a real headless Playwright Java browser and returns a
 * {@link PlaywrightJavaWebDriver} backed by a genuine {@link PlaywrightJavaSession}. It lives in the
 * {@code web.playwright} package because {@link PlaywrightJavaSession}/{@link PlaywrightJavaRuntime}
 * are package-private. Used by unit tests that need to exercise the native Playwright Java path
 * end-to-end (not a mocked {@code Page}).
 */
public final class PlaywrightJavaTestDriverFactory implements AutoCloseable {
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext browserContext;

    public PlaywrightJavaTestDriverFactory() {
        this.playwright = Playwright.create();
        this.browser = playwright.chromium().launch(
                new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
        this.browserContext = browser.newContext();
    }

    public PlaywrightJavaWebDriver createDriver(String userPersona) {
        PlaywrightBrowserConfig browserConfig = new PlaywrightBrowserConfig(
                "chromium", true, List.of("--headless=new"), null, null, Map.of(), Map.of());
        PlaywrightJavaRuntime runtime = new PlaywrightJavaRuntime(playwright, browser);
        String sessionId = UUID.randomUUID().toString();
        Page page = browserContext.newPage();
        PlaywrightJavaSession session = new PlaywrightJavaSession(
                sessionId, userPersona, runtime, browserConfig, browserContext, page,
                Path.of(sessionId + "-trace.zip"),
                Path.of(sessionId + "-network.har"),
                Path.of(sessionId + "-console.log"),
                new CopyOnWriteArrayList<>());
        return new PlaywrightJavaWebDriver(session);
    }

    @Override
    public void close() {
        try {
            browserContext.close();
            browser.close();
        } finally {
            playwright.close();
        }
    }
}
