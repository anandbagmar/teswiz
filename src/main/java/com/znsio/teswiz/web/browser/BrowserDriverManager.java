package com.znsio.teswiz.web.browser;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.exceptions.InvalidTestDataException;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.web.WebEngine;
import com.znsio.teswiz.web.playwright.PlaywrightJavaDriverManager;
import com.znsio.teswiz.web.playwright.PlaywrightWorkerManager;
import com.znsio.teswiz.web.provider.playwright.PlaywrightCloudSessionMetadataResolver;
import com.znsio.teswiz.web.selenium.SeleniumDriverManager;

import java.util.EnumMap;
import java.util.Map;

public final class BrowserDriverManager {
    private static final PlaywrightWorkerManager PLAYWRIGHT_WORKER_MANAGER = new PlaywrightWorkerManager();
    private static final PlaywrightJavaDriverManager PLAYWRIGHT_JAVA_DRIVER_MANAGER = new PlaywrightJavaDriverManager();
    private static final PlaywrightCloudSessionMetadataResolver PLAYWRIGHT_CLOUD_SESSION_METADATA_RESOLVER =
            new PlaywrightCloudSessionMetadataResolver();

    private BrowserDriverManager() {
    }

    public static WebDriverSessionResult createWebSessionForUser(String userPersona, String browserName,
            Platform forPlatform, TestExecutionContext context) {
        return createWebSessionForUser(userPersona, browserName, forPlatform, context, PLAYWRIGHT_WORKER_MANAGER,
                PLAYWRIGHT_JAVA_DRIVER_MANAGER);
    }

    static WebDriverSessionResult createWebSessionForUser(String userPersona, String browserName,
            Platform forPlatform, TestExecutionContext context, PlaywrightWorkerManager playwrightWorkerManager,
            PlaywrightJavaDriverManager playwrightJavaDriverManager) {
        return createWebSessionForUser(userPersona, browserName, forPlatform, context, playwrightWorkerManager,
                playwrightJavaDriverManager, PLAYWRIGHT_CLOUD_SESSION_METADATA_RESOLVER);
    }

    static WebDriverSessionResult createWebSessionForUser(String userPersona, String browserName,
            Platform forPlatform, TestExecutionContext context, PlaywrightWorkerManager playwrightWorkerManager,
            PlaywrightJavaDriverManager playwrightJavaDriverManager,
            PlaywrightCloudSessionMetadataResolver cloudSessionMetadataResolver) {
        String runningOn = Runner.isRunningInCI() ? "CI" : "local";
        context.addTestState(TEST_CONTEXT.WEB_BROWSER_ON, runningOn);
        return driverManagerFor(Runner.getWebEngine(), playwrightWorkerManager, playwrightJavaDriverManager,
                cloudSessionMetadataResolver)
                .createWebSessionForUser(userPersona, browserName, forPlatform, context);
    }

    public static void closeWebDriver(String userPersona, Driver driver) {
        driverManagerFor(Runner.getWebEngine(), PLAYWRIGHT_WORKER_MANAGER, PLAYWRIGHT_JAVA_DRIVER_MANAGER,
                PLAYWRIGHT_CLOUD_SESSION_METADATA_RESOLVER)
                .closeWebDriver(userPersona, driver);
    }

    /**
     * Resolves the {@link WebEngineDriverManager} for the given engine via a registry built from the
     * supplied collaborators, replacing the former per-engine {@code switch} statements. The
     * collaborators are passed in (rather than always using the static singletons) so tests can
     * inject stubbed Playwright managers.
     */
    private static WebEngineDriverManager driverManagerFor(WebEngine webEngine,
            PlaywrightWorkerManager playwrightWorkerManager,
            PlaywrightJavaDriverManager playwrightJavaDriverManager,
            PlaywrightCloudSessionMetadataResolver cloudSessionMetadataResolver) {
        Map<WebEngine, WebEngineDriverManager> registry = new EnumMap<>(WebEngine.class);
        registry.put(WebEngine.SELENIUM, new SeleniumWebEngineDriverManager());
        registry.put(WebEngine.PLAYWRIGHT_JAVA, playwrightJavaDriverManager);
        registry.put(WebEngine.PLAYWRIGHT_TS,
                new PlaywrightTsWebEngineDriverManager(playwrightWorkerManager, cloudSessionMetadataResolver));
        WebEngineDriverManager driverManager = registry.get(webEngine);
        if (null == driverManager) {
            throw new InvalidTestDataException(
                    String.format("Unexpected web engine: '%s'", webEngine.getConfigValue()));
        }
        return driverManager;
    }

    public static Driver createElectronDriverForUser(String userPersona, String browserName,
            Platform forPlatform, TestExecutionContext context) {
        return SeleniumDriverManager.createElectronDriverForUser(userPersona, browserName, forPlatform, context);
    }

    public static void shutdownPlaywrightWorker(TestExecutionContext context) {
        PLAYWRIGHT_WORKER_MANAGER.shutdown(context);
    }
}
