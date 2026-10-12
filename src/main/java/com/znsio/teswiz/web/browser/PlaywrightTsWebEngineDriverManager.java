package com.znsio.teswiz.web.browser;

import java.util.LinkedHashMap;
import java.util.Map;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Drivers;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.session.SessionHandle;
import com.znsio.teswiz.web.playwright.PlaywrightWebDriver;
import com.znsio.teswiz.web.playwright.PlaywrightWorkerManager;
import com.znsio.teswiz.web.provider.playwright.PlaywrightCloudSessionMetadataResolver;
import com.znsio.teswiz.web.selenium.WebBaseUrlResolver;

/**
 * {@link WebEngineDriverManager} for the Playwright-TS engine. Owns the managed-session creation,
 * session-handle enrichment, and close logic that previously lived as private helpers inside
 * {@link BrowserDriverManager}.
 */
public final class PlaywrightTsWebEngineDriverManager implements WebEngineDriverManager {

    private final PlaywrightWorkerManager playwrightWorkerManager;
    private final PlaywrightCloudSessionMetadataResolver cloudSessionMetadataResolver;

    public PlaywrightTsWebEngineDriverManager(PlaywrightWorkerManager playwrightWorkerManager,
                                              PlaywrightCloudSessionMetadataResolver cloudSessionMetadataResolver) {
        this.playwrightWorkerManager = playwrightWorkerManager;
        this.cloudSessionMetadataResolver = cloudSessionMetadataResolver;
    }

    @Override
    public WebDriverSessionResult createWebSessionForUser(String userPersona, String browserName, Platform forPlatform,
                                                          TestExecutionContext context) {
        PlaywrightWorkerManager.ManagedPlaywrightSession managedSession = playwrightWorkerManager
                .createManagedSession(userPersona, browserName, forPlatform, context);
        PlaywrightWebDriver playwrightWebDriver = managedSession.createWebDriver();
        playwrightWebDriver.get(WebBaseUrlResolver.resolve(Drivers.getAppNamefor(userPersona)));
        SessionHandle sessionHandle = enrichSessionHandle(managedSession.sessionHandle(),
                cloudSessionMetadataResolver.resolve(playwrightWebDriver,
                        managedSession.sessionHandle().metadata().get("provider")));
        return new WebDriverSessionResult(playwrightWebDriver,
                Runner.isRunningInHeadlessMode(),
                managedSession.createCapabilities(),
                sessionHandle);
    }

    @Override
    public void closeWebDriver(String userPersona, Driver driver) {
        if (null != driver.getInnerDriver()) {
            driver.getInnerDriver().quit();
        }
    }

    private SessionHandle enrichSessionHandle(SessionHandle sessionHandle, Map<String, String> additionalMetadata) {
        if (additionalMetadata.isEmpty()) {
            return sessionHandle;
        }
        Map<String, String> mergedMetadata = new LinkedHashMap<>(sessionHandle.metadata());
        mergedMetadata.putAll(additionalMetadata);
        return new SessionHandle(sessionHandle.userPersona(), sessionHandle.platform(), sessionHandle.engine(),
                sessionHandle.sessionId(), sessionHandle.artifactPath(), mergedMetadata);
    }
}
