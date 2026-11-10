package com.znsio.teswiz.reporting;

import com.znsio.teswiz.config.TeswizConfig;
import com.znsio.teswiz.runner.DeviceSetup;
import com.znsio.teswiz.runner.Setup;

import java.util.HashMap;
import java.util.Map;

import static com.znsio.teswiz.runner.Setup.*;

// Shared test-execution metadata (environment, platform, build info, aggregated
// session info) shown on the "Features" overview page of a masterthought rich
// report. Used by both Cucumber mode (via CustomReports) and TestNG mode (via
// TestNgCucumberStyleReportWriter) so the two report writers stay consistent.
public final class TestExecutionMetadataBuilder {
    private TestExecutionMetadataBuilder() { }

    public static Map<String, Object> build(String reportsDir) {
        HashMap<String, Object> testRunMetadata = new HashMap<>();
        testRunMetadata.put(FRAMEWORK, TeswizConfig.reader().getString(FRAMEWORK));
        testRunMetadata.put(APP_NAME, TeswizConfig.reader().getString(APP_NAME));
        testRunMetadata.put(TARGET_ENVIRONMENT, TeswizConfig.reader().getString(TARGET_ENVIRONMENT));
        testRunMetadata.put(PLATFORM, TeswizConfig.reader().getString(PLATFORM));
        testRunMetadata.put(WEB_ENGINE, TeswizConfig.reader().getString(WEB_ENGINE));
        testRunMetadata.put(API_ENGINE, TeswizConfig.reader().getString(API_ENGINE));
        testRunMetadata.put(BROWSER, TeswizConfig.reader().getString(BROWSER));
        testRunMetadata.put(TAG, TeswizConfig.reader().getString(TAG_FOR_REPORTPORTAL));
        testRunMetadata.put(RUN_IN_CI, TeswizConfig.reader().getBooleanAsString(RUN_IN_CI));
        testRunMetadata.put("CLOUD_NAME", DeviceSetup.getCloudNameFromCapabilities());
        testRunMetadata.put(EXECUTED_ON, TeswizConfig.reader().getString(EXECUTED_ON));
        testRunMetadata.put(IS_VISUAL, TeswizConfig.reader().getBooleanAsString(IS_VISUAL));
        testRunMetadata.put(IS_OCR_ENABLED, TeswizConfig.reader().getBooleanAsString(IS_OCR_ENABLED));
        testRunMetadata.put(HIGHLIGHT_ELEMENTS, TeswizConfig.reader().getBooleanAsString(HIGHLIGHT_ELEMENTS));
        testRunMetadata.put(SET_HARD_GATE, TeswizConfig.reader().getBooleanAsString(SET_HARD_GATE));
        testRunMetadata.put(IS_FAILING_TEST_SUITE, TeswizConfig.reader().getBooleanAsString(IS_FAILING_TEST_SUITE));
        testRunMetadata.put(PARALLEL, TeswizConfig.reader().getInteger(PARALLEL));
        testRunMetadata.put("OS", System.getProperty("os.name"));
        testRunMetadata.put(HOST_NAME, Setup.getHostMachineName());
        testRunMetadata.put(BRANCH_NAME, TeswizConfig.reader().getString(BRANCH_NAME));
        testRunMetadata.put(BUILD_ID, TeswizConfig.reader().getString(BUILD_ID));
        testRunMetadata.put(BUILD_INITIATION_REASON, TeswizConfig.reader().getString(BUILD_INITIATION_REASON));
        testRunMetadata.putAll(ScenarioSessionMetadataAggregator.aggregate(reportsDir));
        return testRunMetadata;
    }
}
