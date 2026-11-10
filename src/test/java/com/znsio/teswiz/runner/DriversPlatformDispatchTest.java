package com.znsio.teswiz.runner;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.exceptions.InvalidTestDataException;
import com.znsio.teswiz.web.browser.BrowserDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.openqa.selenium.WebDriver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins how {@link Drivers} dispatches driver creation by {@link Platform} and driver closing by
 * driver type. The engine managers are replaced with static mocks that throw a marker naming the
 * method that was called, so no device or browser is needed.
 */
class DriversPlatformDispatchTest {
    private static final String PERSONA = "I";
    private static final String BROWSER = "chrome";

    private MockedStatic<AppiumDriverManager> appiumDriverManager;
    private MockedStatic<BrowserDriverManager> browserDriverManager;
    private TestExecutionContext context;

    @BeforeEach
    void mockEngineManagers() {
        appiumDriverManager = mockStatic(AppiumDriverManager.class);
        browserDriverManager = mockStatic(BrowserDriverManager.class);
        context = new TestExecutionContext("drivers-platform-dispatch");
        appiumDriverManager.when(() -> AppiumDriverManager.createAndroidDriverForUser(anyString(), any(), any()))
                .thenThrow(new MarkerException("createAndroidDriverForUser"));
        appiumDriverManager.when(() -> AppiumDriverManager.createIOSDriverForUser(anyString(), any(), any()))
                .thenThrow(new MarkerException("createIOSDriverForUser"));
        appiumDriverManager.when(() -> AppiumDriverManager.createWindowsDriverForUser(anyString(), any(), any()))
                .thenThrow(new MarkerException("createWindowsDriverForUser"));
        browserDriverManager.when(() -> BrowserDriverManager.createWebSessionForUser(anyString(), anyString(), any(),
                any())).thenThrow(new MarkerException("createWebSessionForUser"));
        browserDriverManager.when(() -> BrowserDriverManager.createElectronDriverForUser(anyString(), anyString(),
                any(), any())).thenThrow(new MarkerException("createElectronDriverForUser"));
    }

    @AfterEach
    void closeStaticMocks() {
        appiumDriverManager.close();
        browserDriverManager.close();
    }

    // --- create ---

    @Test
    void androidIsCreatedByTheAppiumDriverManager() {
        assertCreatedBy(Platform.android, "createAndroidDriverForUser");
    }

    @Test
    void iOSIsCreatedByTheAppiumDriverManager() {
        assertCreatedBy(Platform.iOS, "createIOSDriverForUser");
    }

    @Test
    void windowsIsCreatedByTheAppiumDriverManager() {
        assertCreatedBy(Platform.windows, "createWindowsDriverForUser");
    }

    @Test
    void webIsCreatedAsABrowserSession() {
        assertCreatedBy(Platform.web, "createWebSessionForUser");
    }

    @Test
    void electronIsCreatedByTheBrowserDriverManager() {
        assertCreatedBy(Platform.electron, "createElectronDriverForUser");
    }

    @ParameterizedTest
    @EnumSource(value = Platform.class, names = {"api", "cli", "pdf"})
    void platformsWithoutADriverAreRejected(Platform platform) {
        assertThatThrownBy(() -> Drivers.createDriverForPlatform(PERSONA, BROWSER, platform, context))
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessage(String.format("Unexpected platform value: '%s' provided to assign Driver for user: '%s': ",
                        platform, PERSONA));
    }

    @Test
    void missingPlatformFailsWithNullPointer() {
        assertThatThrownBy(() -> Drivers.createDriverForPlatform(PERSONA, BROWSER, null, context))
                .isInstanceOf(NullPointerException.class);
    }

    // --- close ---

    @Test
    void webDriversAreClosedByTheBrowserDriverManager() {
        Driver driver = driverOfType(Driver.WEB_DRIVER);

        Drivers.attachLogsAndCloseDriver(PERSONA, driver);

        browserDriverManager.verify(() -> BrowserDriverManager.closeWebDriver(PERSONA, driver));
        appiumDriverManager.verify(() -> AppiumDriverManager.closeAppiumDriver(anyString(), any()), never());
    }

    @Test
    void appiumDriversAreClosedByTheAppiumDriverManager() {
        Driver driver = driverOfType(Driver.APPIUM_DRIVER);

        Drivers.attachLogsAndCloseDriver(PERSONA, driver);

        appiumDriverManager.verify(() -> AppiumDriverManager.closeAppiumDriver(PERSONA, driver));
        browserDriverManager.verify(() -> BrowserDriverManager.closeWebDriver(anyString(), any()), never());
    }

    @Test
    void pdfDriversNeedNoClosingAndAreNotAskedForAnInnerDriver() {
        Driver driver = mock(Driver.class);
        when(driver.getType()).thenReturn(Driver.PDF_DRIVER);

        Drivers.attachLogsAndCloseDriver(PERSONA, driver);

        verify(driver, never()).getInnerDriver();
        browserDriverManager.verify(() -> BrowserDriverManager.closeWebDriver(anyString(), any()), never());
        appiumDriverManager.verify(() -> AppiumDriverManager.closeAppiumDriver(anyString(), any()), never());
    }

    @Test
    void unknownDriverTypesAreRejected() {
        Driver driver = driverOfType("other-driver");

        assertThatThrownBy(() -> Drivers.attachLogsAndCloseDriver(PERSONA, driver))
                .isInstanceOf(InvalidTestDataException.class).hasMessage("Unexpected driver type: 'other-driver'");
    }

    private void assertCreatedBy(Platform platform, String expectedManagerMethod) {
        assertThatThrownBy(() -> Drivers.createDriverForPlatform(PERSONA, BROWSER, platform, context))
                .isInstanceOf(MarkerException.class).hasMessage(expectedManagerMethod);
    }

    private static Driver driverOfType(String type) {
        Driver driver = mock(Driver.class);
        when(driver.getType()).thenReturn(type);
        when(driver.getInnerDriver()).thenReturn(mock(WebDriver.class));
        return driver;
    }

    private static final class MarkerException extends RuntimeException {
        MarkerException(String managerMethod) {
            super(managerMethod);
        }
    }
}
