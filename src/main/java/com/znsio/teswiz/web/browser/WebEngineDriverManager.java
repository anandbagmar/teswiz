package com.znsio.teswiz.web.browser;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.runner.Driver;

/**
 * Uniform contract for creating and closing a web browser session for one of the supported web
 * engines (Selenium, Playwright-Java, Playwright-TS). {@link BrowserDriverManager} dispatches to the
 * implementation registered for the active {@link com.znsio.teswiz.web.WebEngine} via a registry,
 * replacing the former per-engine {@code switch} statements with polymorphism.
 */
public interface WebEngineDriverManager {

    /**
     * Creates a web session for the given user persona and returns the resulting driver, headless
     * flag, capabilities, and session handle bundled in a {@link WebDriverSessionResult}.
     */
    WebDriverSessionResult createWebSessionForUser(String userPersona, String browserName, Platform forPlatform,
                                                   TestExecutionContext context);

    /**
     * Closes the web session previously created for the given user persona.
     */
    void closeWebDriver(String userPersona, Driver driver);
}
