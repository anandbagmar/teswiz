package com.znsio.teswiz.runner;

import com.znsio.teswiz.web.playwright.PlaywrightJavaTestDriverFactory;
import org.openqa.selenium.WebDriver;

/**
 * Launches a real headless Playwright Java browser so tests exercise the native Playwright Java path
 * (PlaywrightJavaScreenContext + page().locator(...)) end-to-end, rather than routing through the
 * Playwright TS worker.
 */
final class PlaywrightJavaWebDriverFixture implements SharedWebDriverFixture {
    private final PlaywrightJavaTestDriverFactory driverFactory = new PlaywrightJavaTestDriverFactory();

    @Override
    public WebDriver createDriver(String userPersona) {
        return driverFactory.createDriver(userPersona);
    }

    @Override
    public void close() {
        driverFactory.close();
    }
}
