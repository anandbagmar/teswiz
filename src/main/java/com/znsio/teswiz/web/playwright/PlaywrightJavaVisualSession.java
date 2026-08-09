package com.znsio.teswiz.web.playwright;

import java.util.Map;

import com.applitools.eyes.BatchInfo;
import com.applitools.eyes.FileLogger;
import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.ProxySettings;
import com.applitools.eyes.TestResults;
import com.applitools.eyes.config.Configuration;
import com.applitools.eyes.playwright.ClassicRunner;
import com.applitools.eyes.playwright.Eyes;
import com.applitools.eyes.playwright.fluent.PlaywrightCheckSettings;
import com.applitools.eyes.playwright.fluent.Target;
import com.microsoft.playwright.Page;
import com.znsio.teswiz.visual.PlaywrightCheckSettingsSupport;
import com.znsio.teswiz.visual.PlaywrightVisualSessionRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class PlaywrightJavaVisualSession {
    private static final Logger LOGGER = LogManager.getLogger(PlaywrightJavaVisualSession.class.getName());

    private final Page page;
    private final PlaywrightCheckSettingsSupport checkSettingsSupport = new PlaywrightCheckSettingsSupport();

    private Eyes eyes;

    PlaywrightJavaVisualSession(Page page) {
        this.page = page;
    }

    void open(PlaywrightVisualSessionRequest request) {
        eyes = new Eyes(new ClassicRunner());
        Configuration configuration = new Configuration();
        configuration.setServerUrl(request.serverUrl());
        configuration.setApiKey(request.apiKey());
        configuration.setBranchName(request.branchName());
        configuration.setEnvironmentName(request.environmentName());
        configuration.setMatchLevel(request.defaultMatchLevel());
        configuration.setSaveNewTests(request.saveNewTests());
        setIfPresent(request.baselineEnvName(), configuration::setBaselineEnvName);
        setIfPresent(request.appName(), configuration::setAppName);
        setBatch(configuration, request.batchMetadata());
        eyes.setConfiguration(configuration);
        eyes.setIsDisabled(!request.enabled());
        setIfPresent(request.proxyUrl(), proxyUrl -> eyes.setProxy(new ProxySettings(proxyUrl)));
        setIfPresent(request.logFilePath(), logFilePath -> eyes.setLogHandler(
                new FileLogger(logFilePath, true, request.verboseLogs())));
        addProperties(request.customProperties());
        eyes.open(page, request.appName(), request.testName(), request.viewportSize());
    }

    void checkWindow(String tag) {
        eyes.check(Target.window().withName(tag));
    }

    void check(String tag, com.applitools.eyes.selenium.fluent.SeleniumCheckSettings checkSettings) {
        PlaywrightCheckSettingsSupport.PlaywrightCheckOptions checkOptions = checkSettingsSupport.toCheckOptions(
                checkSettings);
        PlaywrightCheckSettings target = Target.window().withName(tag);
        if (checkOptions.fully()) {
            target = target.fully();
        }
        if (null != checkOptions.matchLevel()) {
            target = target.matchLevel(checkOptions.matchLevel());
        }
        eyes.check(target);
    }

    void checkWindow(String tag, MatchLevel matchLevel) {
        eyes.check(Target.window().withName(tag).matchLevel(matchLevel));
    }

    TestResults close() {
        return eyes.close(false);
    }

    boolean isDisabled() {
        return null == eyes || Boolean.TRUE.equals(eyes.getIsDisabled());
    }

    private void addProperties(Map<String, String> customProperties) {
        for (Map.Entry<String, String> entry : customProperties.entrySet()) {
            eyes.addProperty(entry.getKey(), entry.getValue());
        }
    }

    private void setBatch(Configuration configuration, PlaywrightVisualSessionRequest.BatchMetadata batchMetadata) {
        BatchInfo batchInfo = new BatchInfo(batchMetadata.name());
        batchInfo.setId(batchMetadata.id());
        for (Map.Entry<String, String> entry : batchMetadata.properties().entrySet()) {
            batchInfo.addProperty(entry.getKey(), entry.getValue());
        }
        configuration.setBatch(batchInfo);
    }

    private void setIfPresent(String value, java.util.function.Consumer<String> consumer) {
        if (null != value && !value.isBlank()) {
            consumer.accept(value);
        }
    }
}
