package com.znsio.teswiz.runner;

import java.io.File;

import com.applitools.eyes.BatchInfo;

import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.tools.StringUtils;

/**
 * Builds the Applitools {@link BatchInfo} (name, id, and properties) from configuration.
 *
 * <p>Extracted from {@link Setup}'s {@code setupApplitoolsBatchInfo()} / {@code buildApplitoolsBatchName()}
 * so the batch-naming and batch-property rules live in one named, testable collaborator driven by an
 * injectable {@link TeswizConfiguration}. Behaviour (batch name, suffix handling, {@code batchid}
 * system property, and the attached properties) is preserved exactly.
 */
public final class ApplitoolsBatchInfoFactory {

    private final TeswizConfiguration configuration;

    public ApplitoolsBatchInfoFactory(TeswizConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Builds the batch name for the given platform: {@code <launchName>-<targetEnvironment>}, with a
     * {@code -<platform>-<webEngine>} suffix on web.
     */
    public String buildBatchName(Platform currentPlatform) {
        String batchName = configuration.getString(Setup.LAUNCH_NAME) + "-"
                + configuration.getString(Setup.TARGET_ENVIRONMENT);
        if (Platform.web.equals(currentPlatform)) {
            batchName += "-" + currentPlatform.name() + "-" + configuration.getString(Setup.WEB_ENGINE);
        }
        return batchName;
    }

    /**
     * Builds the {@link BatchInfo}, appending the (optionally space-prefixed) suffix, attaching the
     * standard properties, and setting the {@code batchid} system property.
     */
    public BatchInfo build(Platform currentPlatform, String batchNameSuffix) {
        String batchName = buildBatchName(currentPlatform);
        if (null != batchNameSuffix && !batchNameSuffix.isBlank()) {
            if (!Character.isWhitespace(batchNameSuffix.charAt(0))) {
                batchNameSuffix = " " + batchNameSuffix;
            }
            batchName += batchNameSuffix;
        }

        BatchInfo batchInfo = new BatchInfo(batchName);
        batchInfo.addProperty(Setup.APP_NAME, configuration.getString(Setup.APP_NAME));
        batchInfo.addProperty(Setup.BRANCH_NAME, configuration.getString(Setup.BRANCH_NAME));
        batchInfo.addProperty(Setup.PLATFORM, currentPlatform.name());
        if (Platform.web.equals(currentPlatform)) {
            batchInfo.addProperty(Setup.WEB_ENGINE, configuration.getString(Setup.WEB_ENGINE));
        }
        batchInfo.addProperty(Setup.RUN_IN_CI, configuration.getBooleanAsString(Setup.RUN_IN_CI));
        batchInfo.addProperty(Setup.TARGET_ENVIRONMENT, configuration.getString(Setup.TARGET_ENVIRONMENT));
        batchInfo.addProperty(Setup.REPOSITORY_NAME, new File(System.getProperty("user.dir")).getName());
        String batchId = StringUtils.normaliseScenarioName(configuration.getString(Setup.LOG_DIR));
        System.setProperty("batchid", batchId);
        batchInfo.setId(batchId);
        batchInfo.setNotifyOnCompletion(true);
        return batchInfo;
    }
}
