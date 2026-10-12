package com.znsio.teswiz.web.browser;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.web.selenium.SeleniumDriverManager;

/**
 * {@link WebEngineDriverManager} backed by the static {@link SeleniumDriverManager}. A thin adapter
 * so the Selenium engine participates in the registry dispatch like the other engines.
 */
public final class SeleniumWebEngineDriverManager implements WebEngineDriverManager {

    @Override
    public WebDriverSessionResult createWebSessionForUser(String userPersona, String browserName, Platform forPlatform,
                                                          TestExecutionContext context) {
        return SeleniumDriverManager.createWebSessionForUser(userPersona, browserName, forPlatform, context);
    }

    @Override
    public void closeWebDriver(String userPersona, Driver driver) {
        SeleniumDriverManager.closeWebDriver(userPersona, driver);
    }
}
