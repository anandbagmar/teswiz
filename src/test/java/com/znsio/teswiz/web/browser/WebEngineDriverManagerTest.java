package com.znsio.teswiz.web.browser;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.web.playwright.PlaywrightJavaDriverManager;
import com.znsio.teswiz.web.playwright.PlaywrightWorkerManager;
import com.znsio.teswiz.web.provider.playwright.PlaywrightCloudSessionMetadataResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link WebEngineDriverManager} adapters introduced in place of the per-engine
 * {@code switch} statements. Verifies each engine resolves to its adapter and that the pure close
 * paths behave correctly without a live browser.
 */
class WebEngineDriverManagerTest {

    @Test
    void seleniumAdapterImplementsTheContract() {
        assertThat(new SeleniumWebEngineDriverManager()).isInstanceOf(WebEngineDriverManager.class);
    }

    @Test
    void playwrightJavaManagerImplementsTheContract() {
        assertThat(new PlaywrightJavaDriverManager()).isInstanceOf(WebEngineDriverManager.class);
    }

    @Test
    void playwrightTsAdapterImplementsTheContract() {
        PlaywrightTsWebEngineDriverManager manager = new PlaywrightTsWebEngineDriverManager(
                mock(PlaywrightWorkerManager.class), mock(PlaywrightCloudSessionMetadataResolver.class));

        assertThat(manager).isInstanceOf(WebEngineDriverManager.class);
    }

    @Test
    void playwrightTsAdapterCloseQuitsInnerDriver() {
        PlaywrightTsWebEngineDriverManager manager = new PlaywrightTsWebEngineDriverManager(
                mock(PlaywrightWorkerManager.class), mock(PlaywrightCloudSessionMetadataResolver.class));
        WebDriver innerDriver = mock(WebDriver.class);
        Driver driver = mock(Driver.class);
        when(driver.getInnerDriver()).thenReturn(innerDriver);

        manager.closeWebDriver("buyer", driver);

        verify(innerDriver).quit();
    }

    @Test
    void playwrightTsAdapterCloseIsSafeWhenInnerDriverIsNull() {
        PlaywrightTsWebEngineDriverManager manager = new PlaywrightTsWebEngineDriverManager(
                mock(PlaywrightWorkerManager.class), mock(PlaywrightCloudSessionMetadataResolver.class));
        Driver driver = mock(Driver.class);
        when(driver.getInnerDriver()).thenReturn(null);

        manager.closeWebDriver("buyer", driver);
        // no exception, nothing to quit
    }
}
