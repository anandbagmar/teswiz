package com.znsio.teswiz.runner;

import com.applitools.eyes.BatchInfo;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.entities.Platform;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ApplitoolsBatchInfoFactory}, extracted from {@link Setup}. Locks batch-name
 * composition, web-engine naming, suffix handling, and the attached properties.
 */
class ApplitoolsBatchInfoFactoryTest {

    private TeswizConfiguration configurationWith(Platform platform) {
        TeswizConfiguration configuration = new TeswizConfiguration();
        configuration.putString(Setup.LAUNCH_NAME, "MyApp - web");
        configuration.putString(Setup.TARGET_ENVIRONMENT, "staging");
        configuration.putString(Setup.APP_NAME, "MyApp");
        configuration.putString(Setup.BRANCH_NAME, "main");
        configuration.putString(Setup.WEB_ENGINE, "playwright-ts");
        configuration.putString(Setup.LOG_DIR, "./target/logs/run-1");
        configuration.putBoolean(Setup.RUN_IN_CI, false);
        return configuration;
    }

    @Test
    void buildsBatchNameWithWebEngineSuffixOnWeb() {
        ApplitoolsBatchInfoFactory factory = new ApplitoolsBatchInfoFactory(configurationWith(Platform.web));

        assertThat(factory.buildBatchName(Platform.web)).isEqualTo("MyApp - web-staging-web-playwright-ts");
    }

    @Test
    void buildsBatchNameWithoutWebEngineSuffixOnNonWeb() {
        ApplitoolsBatchInfoFactory factory = new ApplitoolsBatchInfoFactory(configurationWith(Platform.android));

        assertThat(factory.buildBatchName(Platform.android)).isEqualTo("MyApp - web-staging");
    }

    @Test
    void appliesBatchNameSuffixWithLeadingSpaceWhenMissing() {
        ApplitoolsBatchInfoFactory factory = new ApplitoolsBatchInfoFactory(configurationWith(Platform.web));

        BatchInfo batchInfo = factory.build(Platform.web, "nightly");

        assertThat(batchInfo.getName()).isEqualTo("MyApp - web-staging-web-playwright-ts nightly");
    }

    @Test
    void keepsExistingLeadingSpaceInSuffix() {
        ApplitoolsBatchInfoFactory factory = new ApplitoolsBatchInfoFactory(configurationWith(Platform.web));

        BatchInfo batchInfo = factory.build(Platform.web, " nightly");

        assertThat(batchInfo.getName()).isEqualTo("MyApp - web-staging-web-playwright-ts nightly");
    }

    @Test
    void attachesStandardPropertiesAndWebEngineOnWeb() {
        ApplitoolsBatchInfoFactory factory = new ApplitoolsBatchInfoFactory(configurationWith(Platform.web));

        BatchInfo batchInfo = factory.build(Platform.web, "");

        assertThat(batchInfo.getProperties())
                .anySatisfy(p -> assertThat(p).containsEntry("name", Setup.PLATFORM).containsEntry("value", "web"))
                .anySatisfy(p -> assertThat(p).containsEntry("name", Setup.WEB_ENGINE).containsEntry("value", "playwright-ts"))
                .anySatisfy(p -> assertThat(p).containsEntry("name", Setup.APP_NAME).containsEntry("value", "MyApp"));
        assertThat(batchInfo.getId()).isNotBlank();
        assertThat(System.getProperty("batchid")).isNotBlank();
    }
}
