package com.znsio.teswiz.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Post Phase 7 the Playwright TS engine is authored natively via its {@code .screen.ts} modules and no
 * longer honours the Selenium {@code By} locator contract that {@link SeleniumWebDriverTest} asserts
 * (that contract is Selenium-specific). This test therefore asserts the pw-ts engine's actual contract:
 * <ul>
 *   <li>engine-neutral driver operations (navigation, screenshot, window sizing, cookies, browser logs)
 *       still work natively, and</li>
 *   <li>every {@code By}-based element-location / element-handle operation fails fast with the structured
 *       {@code ELEMENT_LOCATION_BY_SELENIUM_LOCATOR} capability error that points authors at native
 *       Playwright usage. These assertions are the regression guard against the removed {@code By} bridge
 *       silently returning.</li>
 * </ul>
 */
class PlaywrightWebDriverTest {
    private PlaywrightWebDriverFixture fixture;

    @AfterEach
    void tearDownFixture() {
        if (null != fixture) {
            fixture.close();
        }
    }

    private WebDriver createDriver(String userPersona) {
        fixture = new PlaywrightWebDriverFixture();
        return fixture.createDriver(userPersona);
    }

    // --- Surviving native driver operations (no By involved) ---

    @Test
    void shouldNavigateCapturePageSourceAndTakeScreenshot() throws Exception {
        WebDriver driver = createDriver("buyer");
        SharedWebDriverContract.assertNavigationPageSourceAndScreenshot(driver,
                SharedWebDriverTestPages.writeTestPage());
    }

    @Test
    void shouldSupportWindowSizingApis() throws Exception {
        WebDriver driver = createDriver("window-user");
        SharedWebDriverContract.assertWindowHandling(driver, SharedWebDriverTestPages.writeTestPage());
    }

    @Test
    void shouldExposeBrowserLogsThroughManageLogs() throws Exception {
        WebDriver driver = createDriver("logs-user");
        SharedWebDriverContract.assertBrowserLogs(driver, SharedWebDriverTestPages.writeConsoleLogPage());
    }

    @Test
    void shouldManageCookies() throws Exception {
        try (SharedWebDriverContract.LocalCookieServer server = new SharedWebDriverContract.LocalCookieServer()) {
            WebDriver driver = createDriver("cookie-user");
            SharedWebDriverContract.assertCookieHandling(driver, server.url());
        }
    }

    // --- Removed By bridge: must fail fast with the capability error ---

    @Test
    void shouldRejectFindElementByWithNativeAuthoringGuidance() throws Exception {
        WebDriver driver = createDriver("locator-user");
        driver.get(SharedWebDriverTestPages.writeTestPage().toUri().toString());

        assertThatThrownBy(() -> driver.findElement(By.id("name")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("ELEMENT_LOCATION_BY_SELENIUM_LOCATOR")
                .hasMessageContaining(".screen.ts");
    }

    @Test
    void shouldRejectFindElementsByWithNativeAuthoringGuidance() throws Exception {
        WebDriver driver = createDriver("locators-user");
        driver.get(SharedWebDriverTestPages.writeTestPage().toUri().toString());

        assertThatThrownBy(() -> driver.findElements(By.cssSelector(".item")))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("ELEMENT_LOCATION_BY_SELENIUM_LOCATOR");
    }

    @Test
    void shouldRejectSwitchToFrameUsingWebElement() throws Exception {
        WebDriver driver = createDriver("frame-element-user");
        driver.get(SharedWebDriverTestPages.writeFrameTestPage().toUri().toString());
        WebElement detachedFrameElement = org.mockito.Mockito.mock(WebElement.class);

        assertThatThrownBy(() -> driver.switchTo().frame(detachedFrameElement))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("ELEMENT_LOCATION_BY_SELENIUM_LOCATOR");
    }
}
