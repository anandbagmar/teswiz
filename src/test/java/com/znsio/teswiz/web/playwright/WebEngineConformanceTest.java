package com.znsio.teswiz.web.playwright;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ViewportSize;
import com.znsio.teswiz.config.browser.PlaywrightBrowserConfig;
import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.runner.Setup;

class WebEngineConformanceTest {
    private static final String CONFIG_FILE = "./configs/theapp/theapp_local_web_config.properties";

    @BeforeEach
    void setUp() {
        Setup.load(CONFIG_FILE);
        Setup.loadAndUpdateConfigParameters(CONFIG_FILE);
    }

    @AfterEach
    void tearDown() {
        SessionContext.remove(Thread.currentThread().getId());
    }

    @Test
    void shouldSupportCookieManagementAcrossPlaywrightJava() {
        BrowserContext browserContext = mock(BrowserContext.class);
        Page page = mock(Page.class);
        com.microsoft.playwright.options.Cookie pwCookie = new com.microsoft.playwright.options.Cookie("test_key", "test_val");
        when(browserContext.cookies()).thenReturn(List.of(pwCookie));

        PlaywrightJavaWebDriver driver = new PlaywrightJavaWebDriver(createSession(page, browserContext));

        driver.manage().addCookie(new Cookie("test_key", "test_val"));
        verify(browserContext).addCookies(anyList());

        Set<Cookie> cookies = driver.manage().getCookies();
        assertThat(cookies).extracting(Cookie::getName).contains("test_key");

        driver.manage().deleteCookieNamed("test_key");
        verify(browserContext).clearCookies();
    }

    @Test
    void shouldSupportViewportResizingInPlaywrightJava() {
        BrowserContext browserContext = mock(BrowserContext.class);
        Page page = mock(Page.class);
        when(page.viewportSize()).thenReturn(new ViewportSize(1024, 768));

        PlaywrightJavaWebDriver driver = new PlaywrightJavaWebDriver(createSession(page, browserContext));

        driver.manage().window().setSize(new Dimension(1024, 768));
        verify(page).setViewportSize(1024, 768);

        Dimension size = driver.manage().window().getSize();
        assertThat(size.getWidth()).isEqualTo(1024);
        assertThat(size.getHeight()).isEqualTo(768);
    }

    @Test
    void shouldSupportAsyncScriptExecutionInPlaywrightJava() {
        BrowserContext browserContext = mock(BrowserContext.class);
        Page page = mock(Page.class);
        when(page.evaluate(anyString(), any())).thenReturn("async-ok");

        PlaywrightJavaWebDriver driver = new PlaywrightJavaWebDriver(createSession(page, browserContext));

        Object result = driver.executeAsyncScript(
                "var callback = arguments[arguments.length - 1];" +
                "setTimeout(function() { callback('async-ok'); }, 50);");

        assertThat(result).isEqualTo("async-ok");
    }

    @Test
    void shouldThrowStructuredDiagnosticExceptionForUnsupportedOperations() {
        BrowserContext browserContext = mock(BrowserContext.class);
        Page page = mock(Page.class);

        PlaywrightJavaWebDriver driver = new PlaywrightJavaWebDriver(createSession(page, browserContext));

        assertThatThrownBy(() -> driver.manage().window().setPosition(new Point(100, 100)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("[TESWIZ CAPABILITY ERROR]")
                .hasMessageContaining("WINDOW_POSITION")
                .hasMessageContaining("WEB_ENGINE=playwright-java")
                .hasMessageContaining("HOW TO RESOLVE:");
    }

    private PlaywrightJavaSession createSession(Page page, BrowserContext browserContext) {
        return new PlaywrightJavaSession(
                "session-1",
                "buyer",
                new PlaywrightJavaRuntime(mock(Playwright.class), mock(Browser.class)),
                new PlaywrightBrowserConfig("chrome", true, List.of("--headless=new"), null, null, Map.of(), Map.of()),
                browserContext,
                page,
                Path.of("trace.zip"),
                Path.of("network.har"),
                Path.of("console.log"),
                List.of());
    }
}
