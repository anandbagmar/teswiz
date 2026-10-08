package com.znsio.teswiz.runner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.tools.OsUtils;

/**
 * Builds the ReportPortal execution-attribute string and publishes the {@code System} properties
 * (ATD + ReportPortal.io) that downstream tooling reads.
 *
 * <p>Extracted from {@link Setup}'s {@code setupExecutionEnvironment()} so the attribute formatting
 * and {@code System.setProperty} side effects live in one named collaborator driven by an injectable
 * {@link TeswizConfiguration}. Behaviour (attribute string format, property keys/values) is
 * preserved exactly.
 */
public final class ReportPortalEnvironmentPublisher {

    private static final Logger LOGGER = LogManager.getLogger(ReportPortalEnvironmentPublisher.class.getName());

    private final TeswizConfiguration configuration;
    private final String rpDescriptionKey;

    public ReportPortalEnvironmentPublisher(TeswizConfiguration configuration, String rpDescriptionKey) {
        this.configuration = configuration;
        this.rpDescriptionKey = rpDescriptionKey;
    }

    /**
     * Formats the ReportPortal attribute string, logs it, and sets the ATD + ReportPortal.io
     * {@code System} properties.
     *
     * @param currentPlatform the resolved execution platform
     * @param providerName    the cloud provider name (already normalized to "local" when unset)
     */
    public void publish(Platform currentPlatform, String providerName) {
        String rpAttributes = buildAttributes(currentPlatform, providerName);
        LOGGER.info("ReportPortal Test Execution Attributes: {}", rpAttributes);
        publishAtdProperties(currentPlatform);
        publishReportPortalProperties(currentPlatform, rpAttributes);
    }

    private String buildAttributes(Platform currentPlatform, String providerName) {
        String rpAttributes = String.format(
                "AutomationBranch:%s; ExecutedOn:%s; Installer:%s; OS:%s; ParallelCount:%d; " +
                "Platform:%s; RunInCI:%s; Tags:%s; TargetEnvironment:%s; Username:%s; " +
                "VisualEnabled:%s; BuildInitiationReason:%s; ",
                configuration.getString(Setup.BRANCH_NAME), configuration.getString(Setup.EXECUTED_ON),
                configuration.getString(Setup.APP_PATH), OsUtils.getOsName(),
                configuration.getInteger(Setup.PARALLEL), currentPlatform.name(),
                configuration.getBoolean(Setup.RUN_IN_CI),
                configuration.getString(Setup.TAG_FOR_REPORTPORTAL), configuration.getString(Setup.TARGET_ENVIRONMENT),
                OsUtils.getUserName(),
                configuration.getBoolean(Setup.IS_VISUAL), configuration.getString(Setup.BUILD_INITIATION_REASON));

        if (currentPlatform.equals(Platform.web) || currentPlatform.equals(Platform.electron)) {
            rpAttributes += String.format("WebEngine:%s; ", configuration.getString(Setup.WEB_ENGINE));
        }

        rpAttributes += String.format("Provider:%s; ", providerName);

        if (!configuration.getString(Setup.APP_VERSION).equals(Runner.NOT_SET)) {
            rpAttributes += String.format("AppVersion: %s; ", configuration.getString(Setup.APP_VERSION));
        }

        if (!configuration.getString(Setup.BUILD_ID).equals(Runner.NOT_SET)) {
            rpAttributes += String.format("BuildId: %s; ", configuration.getString(Setup.BUILD_ID));
        }
        return rpAttributes;
    }

    private void publishAtdProperties(Platform currentPlatform) {
        System.setProperty(Setup.CLOUD_USERNAME, configuration.getString(Setup.CLOUD_USERNAME));
        System.setProperty(Setup.CLOUD_KEY, configuration.getString(Setup.CLOUD_KEY));
        System.setProperty(Setup.CONFIG_FILE, configuration.getString(Setup.CONFIG_FILE));
        System.setProperty(Setup.CAPS, configuration.getString(Setup.CAPS));
        System.setProperty("Platform", currentPlatform.name());
        System.setProperty("atd_" + currentPlatform.name() + "_app_local", configuration.getString(Setup.APP_PATH));
        if (null != configuration.getString(Setup.PROXY_URL)) {
            System.setProperty(Setup.PROXY_URL, configuration.getString(Setup.PROXY_URL));
        }
    }

    private void publishReportPortalProperties(Platform currentPlatform, String rpAttributes) {
        System.setProperty("rp.description", configuration.getString(Setup.APP_NAME) + " "
                + configuration.getString(rpDescriptionKey) + " on " + currentPlatform.name());
        System.setProperty("rp.launch", configuration.getString(Setup.LAUNCH_NAME));
        System.setProperty("rp.attributes", rpAttributes);
    }
}
