package com.znsio.teswiz.web.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.ViewportSize;
import com.znsio.teswiz.config.browser.PlaywrightBrowserConfig;
import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.runner.Setup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.logging.LogType;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the Selenium {@code switchTo()}, {@code navigate()} and {@code manage()} behaviour of
 * {@link PlaywrightJavaWebDriver} against mocked Playwright objects. Where a call changes the active
 * search root (frames), the effect is observed through {@code executeScript}, which evaluates
 * against that root.
 */
class PlaywrightJavaWebDriverSeleniumApiTest {
    private static final String CONFIG_FILE = "./configs/theapp/theapp_local_web_config.properties";
    private static final String SESSION_ID = "session-1";
    private static final String SCRIPT = "return 1;";

    private Page page;
    private BrowserContext browserContext;
    private PlaywrightJavaWebDriver driver;

    @BeforeEach
    void setUp() {
        Setup.load(CONFIG_FILE);
        Setup.loadAndUpdateConfigParameters(CONFIG_FILE);
        page = mock(Page.class);
        browserContext = mock(BrowserContext.class);
        driver = new PlaywrightJavaWebDriver(createSession());
    }

    @AfterEach
    void tearDown() {
        SessionContext.remove(Thread.currentThread().getId());
    }

    // --- switchTo() ---

    @Test
    void frameByIndexSkipsTheMainFrameAndReturnsTheDriver() {
        Frame mainFrame = mock(Frame.class);
        Frame childFrame = mock(Frame.class);
        when(page.frames()).thenReturn(List.of(mainFrame, childFrame));

        WebDriver returned = driver.switchTo().frame(0);
        driver.executeScript(SCRIPT);

        assertThat(returned).isSameAs(driver);
        verify(childFrame).evaluate(anyString(), any());
        verify(page, never()).evaluate(anyString(), any());
    }

    @Test
    void frameByIndexOutOfRangeThrowsNoSuchFrame() {
        when(page.frames()).thenReturn(List.of(mock(Frame.class)));

        assertThatThrownBy(() -> driver.switchTo().frame(0)).isInstanceOf(NoSuchFrameException.class)
                .hasMessageContaining("No frame found for index: 0");
        assertThatThrownBy(() -> driver.switchTo().frame(-1)).isInstanceOf(NoSuchFrameException.class);
    }

    @Test
    void frameByNameUsesTheNamedFrameWhenPresent() {
        Frame namedFrame = mock(Frame.class);
        when(page.frame("details")).thenReturn(namedFrame);

        driver.switchTo().frame("details");
        driver.executeScript(SCRIPT);

        verify(namedFrame).evaluate(anyString(), any());
    }

    @Test
    void frameByNameFallsBackToAFrameLocatorMatchingNameOrId() {
        FrameLocator frameLocator = mockFrameLocatorRoot();
        when(page.frame("details")).thenReturn(null);
        when(page.frameLocator("iframe[name='details'], iframe#details, frame[name='details'], frame#details"))
                .thenReturn(frameLocator);

        driver.switchTo().frame("details");
        driver.executeScript(SCRIPT);

        verify(frameLocator.locator(":root")).evaluate(anyString(), any());
    }

    @Test
    void frameByPlaywrightElementUsesItsContentFrame() {
        Locator elementLocator = mock(Locator.class);
        FrameLocator contentFrame = mockFrameLocatorRoot();
        when(elementLocator.contentFrame()).thenReturn(contentFrame);

        driver.switchTo().frame(new PlaywrightJavaWebElement(elementLocator, Duration.ZERO));
        driver.executeScript(SCRIPT);

        verify(contentFrame.locator(":root")).evaluate(anyString(), any());
    }

    @Test
    void frameByForeignElementFallsBackToTheFirstFrame() {
        FrameLocator anyFrame = mockFrameLocatorRoot();
        when(page.frameLocator("iframe, frame")).thenReturn(anyFrame);

        driver.switchTo().frame(mock(WebElement.class));
        driver.executeScript(SCRIPT);

        verify(anyFrame.locator(":root")).evaluate(anyString(), any());
    }

    @Test
    void parentFrameMovesToTheParentFrame() {
        Frame parentFrame = mock(Frame.class);
        Frame childFrame = mock(Frame.class);
        when(childFrame.parentFrame()).thenReturn(parentFrame);
        when(page.frames()).thenReturn(List.of(mock(Frame.class), childFrame));

        driver.switchTo().frame(0).switchTo().parentFrame();
        driver.executeScript(SCRIPT);

        verify(parentFrame).evaluate(anyString(), any());
    }

    @Test
    void parentFrameOfATopLevelFrameReturnsToThePage() {
        Frame childFrame = mock(Frame.class);
        when(childFrame.parentFrame()).thenReturn(null);
        when(page.frames()).thenReturn(List.of(mock(Frame.class), childFrame));

        driver.switchTo().frame(0);
        driver.switchTo().parentFrame();
        driver.executeScript(SCRIPT);

        verify(page).evaluate(anyString(), any());
    }

    @Test
    void parentFrameFromAFrameLocatorReturnsToThePage() {
        FrameLocator anyFrame = mockFrameLocatorRoot();
        when(page.frameLocator("iframe, frame")).thenReturn(anyFrame);

        driver.switchTo().frame(mock(WebElement.class));
        assertThat(driver.switchTo().parentFrame()).isSameAs(driver);
        driver.executeScript(SCRIPT);

        verify(page).evaluate(anyString(), any());
    }

    @Test
    void defaultContentReturnsToThePage() {
        Frame namedFrame = mock(Frame.class);
        when(page.frame("details")).thenReturn(namedFrame);

        driver.switchTo().frame("details");
        assertThat(driver.switchTo().defaultContent()).isSameAs(driver);
        driver.executeScript(SCRIPT);

        verify(page).evaluate(anyString(), any());
        verify(namedFrame, never()).evaluate(anyString(), any());
    }

    @Test
    void windowAcceptsOnlyTheCurrentHandle() {
        assertThat(driver.switchTo().window(SESSION_ID)).isSameAs(driver);

        assertThatThrownBy(() -> driver.switchTo().window("other-window"))
                .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("MULTIPLE_WINDOWS");
    }

    @Test
    void newWindowIsUnsupported() {
        assertThatThrownBy(() -> driver.switchTo().newWindow(WindowType.TAB))
                .isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("MULTIPLE_WINDOWS");
    }

    @Test
    void alertReportsNoAlertWithACapabilityDiagnostic() {
        assertThatThrownBy(() -> driver.switchTo().alert()).isInstanceOf(NoAlertPresentException.class)
                .hasMessageContaining("[TESWIZ CAPABILITY ERROR]").hasMessageContaining("ALERTS");
    }

    @Test
    void activeElementLocatesTheFocusedElement() {
        when(page.locator(":focus")).thenReturn(mock(Locator.class));

        assertThat(driver.switchTo().activeElement()).isInstanceOf(PlaywrightJavaWebElement.class);
        verify(page).locator(":focus");
    }

    // --- navigate() ---

    @Test
    void navigateDelegatesToThePage() throws Exception {
        driver.navigate().back();
        driver.navigate().forward();
        driver.navigate().refresh();
        driver.navigate().to("https://example.com/a");
        driver.navigate().to(new URL("https://example.com/b"));

        verify(page).goBack();
        verify(page).goForward();
        verify(page).reload();
        verify(page).navigate(eq("https://example.com/a"), any(Page.NavigateOptions.class));
        verify(page).navigate(eq("https://example.com/b"), any(Page.NavigateOptions.class));
    }

    // --- manage(): cookies ---

    @Test
    void addCookieMapsEveryField() {
        Date expiry = new Date(1_900_000_000_000L);
        Cookie cookie = new Cookie.Builder("sessionId", "abc").domain("example.com").path("/app").expiresOn(expiry)
                .isSecure(true).isHttpOnly(true).build();

        driver.manage().addCookie(cookie);

        com.microsoft.playwright.options.Cookie added = captureSingleAddedCookie();
        assertThat(added.name).isEqualTo("sessionId");
        assertThat(added.value).isEqualTo("abc");
        assertThat(added.domain).isEqualTo("example.com");
        assertThat(added.path).isEqualTo("/app");
        assertThat(added.expires).isEqualTo(1_900_000_000.0);
        assertThat(added.secure).isTrue();
        assertThat(added.httpOnly).isTrue();
    }

    @Test
    void addCookieLeavesUnsetOptionalFieldsEmpty() {
        driver.manage().addCookie(new Cookie.Builder("theme", "dark").build());

        com.microsoft.playwright.options.Cookie added = captureSingleAddedCookie();
        assertThat(added.domain).isNull();
        assertThat(added.expires).isNull();
        assertThat(added.secure).isFalse();
        assertThat(added.httpOnly).isFalse();
    }

    @Test
    void getCookiesMapsPlaywrightCookiesToSelenium() {
        com.microsoft.playwright.options.Cookie full = new com.microsoft.playwright.options.Cookie("a", "1")
                .setDomain("example.com").setPath("/").setExpires(1_900_000_000.0).setSecure(true)
                .setHttpOnly(true);
        com.microsoft.playwright.options.Cookie minimal = new com.microsoft.playwright.options.Cookie("b", "2")
                .setDomain("example.com").setPath("/");
        when(browserContext.cookies()).thenReturn(List.of(full, minimal));

        Set<Cookie> cookies = driver.manage().getCookies();

        Cookie a = cookies.stream().filter(c -> c.getName().equals("a")).findFirst().orElseThrow();
        assertThat(a.getValue()).isEqualTo("1");
        assertThat(a.getDomain()).isEqualTo("example.com");
        assertThat(a.getExpiry()).isEqualTo(new Date(1_900_000_000_000L));
        assertThat(a.isSecure()).isTrue();
        assertThat(a.isHttpOnly()).isTrue();
        Cookie b = cookies.stream().filter(c -> c.getName().equals("b")).findFirst().orElseThrow();
        assertThat(b.getExpiry()).isNull();
        assertThat(b.isSecure()).isFalse();
        assertThat(b.isHttpOnly()).isFalse();
    }

    @Test
    void getCookieNamedFindsByNameOrReturnsNull() {
        when(browserContext.cookies()).thenReturn(List.of(new com.microsoft.playwright.options.Cookie("a", "1")
                .setDomain("example.com").setPath("/")));

        assertThat(driver.manage().getCookieNamed("a").getValue()).isEqualTo("1");
        assertThat(driver.manage().getCookieNamed("missing")).isNull();
    }

    @Test
    void deleteCookieNamedClearsAndRestoresTheOtherCookies() {
        com.microsoft.playwright.options.Cookie keep = new com.microsoft.playwright.options.Cookie("keep", "1");
        com.microsoft.playwright.options.Cookie drop = new com.microsoft.playwright.options.Cookie("drop", "2");
        when(browserContext.cookies()).thenReturn(List.of(keep, drop));

        driver.manage().deleteCookieNamed("drop");

        verify(browserContext).clearCookies();
        verify(browserContext).addCookies(List.of(keep));
    }

    @Test
    void deleteTheOnlyCookieDoesNotReAddAnything() {
        when(browserContext.cookies()).thenReturn(List.of(new com.microsoft.playwright.options.Cookie("only", "1")));

        driver.manage().deleteCookie(new Cookie("only", "1"));

        verify(browserContext).clearCookies();
        verify(browserContext, never()).addCookies(anyList());
    }

    @Test
    void deleteAllCookiesClearsTheContext() {
        driver.manage().deleteAllCookies();

        verify(browserContext).clearCookies();
    }

    // --- manage(): timeouts ---

    @Test
    void timeoutsAreStoredAndChainable() {
        WebDriver.Timeouts timeouts = driver.manage().timeouts();

        assertThat(timeouts.implicitlyWait(Duration.ofMillis(900))).isSameAs(timeouts);
        assertThat(timeouts.pageLoadTimeout(Duration.ofSeconds(12))).isSameAs(timeouts);
        assertThat(timeouts.scriptTimeout(Duration.ofSeconds(7))).isSameAs(timeouts);

        WebDriver.Timeouts reread = driver.manage().timeouts();
        assertThat(reread.getImplicitWaitTimeout()).isEqualTo(Duration.ofMillis(900));
        assertThat(reread.getPageLoadTimeout()).isEqualTo(Duration.ofSeconds(12));
        assertThat(reread.getScriptTimeout()).isEqualTo(Duration.ofSeconds(7));
    }

    @Test
    void pageLoadTimeoutIsUsedForNavigation() {
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(12));
        ArgumentCaptor<Page.NavigateOptions> options = ArgumentCaptor.forClass(Page.NavigateOptions.class);

        driver.get("https://example.com");

        verify(page).navigate(eq("https://example.com"), options.capture());
        assertThat(options.getValue().timeout).isEqualTo(12_000.0);
    }

    // --- manage(): window ---

    @Test
    void windowSizeComesFromTheViewportWhenSet() {
        when(page.viewportSize()).thenReturn(new ViewportSize(1024, 768));

        assertThat(driver.manage().window().getSize()).isEqualTo(new Dimension(1024, 768));
        verify(page, never()).evaluate(anyString());
    }

    @Test
    void windowSizeFallsBackToTheInnerWindowSize() {
        when(page.viewportSize()).thenReturn(null);
        when(page.evaluate("() => window.innerWidth")).thenReturn(1440);
        when(page.evaluate("() => window.innerHeight")).thenReturn("900");

        assertThat(driver.manage().window().getSize()).isEqualTo(new Dimension(1440, 900));
    }

    @Test
    void windowSizeDefaultsWhenTheBrowserCannotReportIt() {
        when(page.viewportSize()).thenReturn(null);
        when(page.evaluate("() => window.innerWidth")).thenThrow(new RuntimeException("closed"));

        assertThat(driver.manage().window().getSize()).isEqualTo(new Dimension(1280, 960));
    }

    @Test
    void windowSizeDefaultsWhenTheBrowserReportsNothing() {
        when(page.viewportSize()).thenReturn(null);
        when(page.evaluate("() => window.innerWidth")).thenReturn(null);

        assertThat(driver.manage().window().getSize()).isEqualTo(new Dimension(1280, 960));
    }

    @Test
    void setSizeResizesTheViewport() {
        driver.manage().window().setSize(new Dimension(900, 700));

        verify(page).setViewportSize(900, 700);
    }

    @Test
    void maximizeResizesTheViewportToTheAvailableScreen() {
        when(page.evaluate("() => window.screen.availWidth")).thenReturn(1920);
        when(page.evaluate("() => window.screen.availHeight")).thenReturn(1080);

        driver.manage().window().maximize();

        verify(page).setViewportSize(1920, 1080);
    }

    @Test
    void maximizeIgnoresBrowserFailures() {
        when(page.evaluate("() => window.screen.availWidth")).thenThrow(new RuntimeException("closed"));

        driver.manage().window().maximize();

        verify(page, never()).setViewportSize(anyInt(), anyInt());
    }

    @Test
    void maximizeDoesNothingWhenTheScreenSizeIsUnknown() {
        when(page.evaluate("() => window.screen.availWidth")).thenReturn(null);

        driver.manage().window().maximize();

        verify(page, never()).setViewportSize(anyInt(), anyInt());
    }

    @Test
    void positionIsAlwaysTheOriginAndMinimizeAndFullscreenAreNoOps() {
        WebDriver.Window window = driver.manage().window();

        assertThat(window.getPosition()).isEqualTo(new Point(0, 0));
        window.minimize();
        window.fullscreen();

        verify(page, never()).setViewportSize(anyInt(), anyInt());
        verify(page, never()).evaluate(anyString());
    }

    // --- manage(): logs ---

    @Test
    void logsExposeOnlyAnEmptyBrowserLog() {
        assertThat(driver.manage().logs().getAvailableLogTypes()).containsExactly(LogType.BROWSER);
        assertThat(driver.manage().logs().get(LogType.BROWSER).getAll()).isEmpty();
        assertThat(driver.manage().logs().get(LogType.PERFORMANCE).getAll()).isEmpty();
    }

    private com.microsoft.playwright.options.Cookie captureSingleAddedCookie() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.microsoft.playwright.options.Cookie>> added = ArgumentCaptor.forClass(List.class);
        verify(browserContext).addCookies(added.capture());
        assertThat(added.getValue()).hasSize(1);
        return added.getValue().get(0);
    }

    private FrameLocator mockFrameLocatorRoot() {
        FrameLocator frameLocator = mock(FrameLocator.class);
        Locator root = mock(Locator.class);
        when(frameLocator.locator(":root")).thenReturn(root);
        return frameLocator;
    }

    private PlaywrightJavaSession createSession() {
        return new PlaywrightJavaSession(SESSION_ID, "buyer",
                new PlaywrightJavaRuntime(mock(Playwright.class), mock(Browser.class)),
                new PlaywrightBrowserConfig("chrome", true, List.of("--headless=new"), null, null, Map.of(), Map.of()),
                browserContext, page, Path.of("trace.zip"), Path.of("network.har"), Path.of("console.log"),
                List.of());
    }
}
