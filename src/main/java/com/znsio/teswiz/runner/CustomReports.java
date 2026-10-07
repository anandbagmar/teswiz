package com.znsio.teswiz.runner;

import com.znsio.teswiz.reporting.TestExecutionMetadataBuilder;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.Reportable;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static com.znsio.teswiz.runner.Setup.*;

class CustomReports {
    private static final Logger LOGGER = LogManager.getLogger(CustomReports.class.getName());

    private CustomReports() {
        LOGGER.debug("CustomReports - private constructor");
    }

    static Reportable generateReport() {
        String reportsDir = Setup.getFromConfigs(LOG_DIR) + File.separator + REPORTS_DIR;
        List<String> jsonPaths = processTestResultJsonFiles(reportsDir);

        if (jsonPaths.isEmpty() || !hasExecutedScenarios(jsonPaths)) {
            LOGGER.info("================================================================================================");
            LOGGER.info("No Cucumber test scenarios matched the tag criteria. Skipping rich report generation.");
            LOGGER.info("================================================================================================");
            return null;
        }

        LOGGER.info(
                "================================================================================================");
        LOGGER.info("Generating reports here: '{}'", reportsDir);
        LOGGER.info(
                "================================================================================================");

        Configuration config = createCucumberReportsConfiguration(reportsDir);

        ReportBuilder reportBuilder = new ReportBuilder(jsonPaths, config);
        Reportable overviewReport = reportBuilder.generateReports();
        if (overviewReport != null) {
            String generatedReportsMessage = String.format(
                    "Reports available here: file://%s/cucumber-html-reports/overview-features.html",
                    config.getReportDirectory().getAbsolutePath());
            LOGGER.info(generatedReportsMessage);
        }
        return overviewReport;
    }

    private static boolean hasExecutedScenarios(List<String> jsonPaths) {
        for (String jsonPath : jsonPaths) {
            try {
                File file = new File(jsonPath);
                if (file.exists() && file.length() > 5) {
                    String content = FileUtils.readFileToString(file, java.nio.charset.StandardCharsets.UTF_8).trim();
                    if (content.startsWith("[") && !content.equals("[]") && !content.equals("[ ]")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Error checking test result json file {}: {}", jsonPath, e.getMessage());
            }
        }
        return false;
    }

    @NotNull
    private static Configuration createCucumberReportsConfiguration(String reportsDir) {
        String richReportsPath = reportsDir + File.separator + "richReports";
        LOGGER.info("\tCreating rich reports: {}", richReportsPath);
        Configuration config = new Configuration(new File(richReportsPath),
                                                 Setup.getFromConfigs(APP_NAME));
        return addTestExecutionMetaDataToReportConfig(excludeCustomTagsFromReport(config), reportsDir);
    }

    private static Configuration excludeCustomTagsFromReport(Configuration config) {
        String tagsToExclude = System.getProperty(
                TEST_CONTEXT.TAGS_TO_EXCLUDE_FROM_CUCUMBER_REPORT);
        if (null != tagsToExclude) {
            config.setTagsToExcludeFromChart(tagsToExclude.trim().split(","));
        }
        return config;
    }

    @NotNull
    static List<String> processTestResultJsonFiles(String reportsDir) {
        Collection<File> jsonFiles = FileUtils.listFiles(new File(reportsDir), new String[]{"json"},
                                                         true);
        List<File> cucumberJsonFiles = jsonFiles.stream()
                .filter(CustomReports::isCucumberResultJsonFile)
                .sorted(Comparator.comparing(File::getAbsolutePath))
                .toList();
        LOGGER.info("\tFound '{}' Cucumber result files for processing", cucumberJsonFiles.size());
        if (cucumberJsonFiles.isEmpty()) {
            LOGGER.info("Reports not generated");
        }
        List<String> jsonPaths = new ArrayList<>(cucumberJsonFiles.size());
        cucumberJsonFiles.forEach(file -> {
            LOGGER.info("\tProcessing result file: {}", file.getAbsolutePath());
            jsonPaths.add(file.getAbsolutePath());
        });
        return jsonPaths;
    }

    private static boolean isCucumberResultJsonFile(File file) {
        String fileName = file.getName();
        return fileName.startsWith("cucumber-") && fileName.endsWith(".json");
    }

    private static Configuration addTestExecutionMetaDataToReportConfig(Configuration config, String reportsDir) {
        Map<String, Object> testRunMetadata = buildTestRunMetadata(reportsDir);

        LOGGER.info("Added test execution metadata to cucumber reports:");

        Map<String, List<String>> categories = new LinkedHashMap<>();
        categories.put("Execution Framework & Application", List.of(FRAMEWORK, APP_NAME, PLATFORM, BRANCH_NAME));
        categories.put("Infrastructure & Environment", List.of(TARGET_ENVIRONMENT, EXECUTED_ON, "CLOUD_NAME", HOST_NAME, "OS", RUN_IN_CI));
        categories.put("Engines & Browsers", List.of(WEB_ENGINE, API_ENGINE, BROWSER));
        categories.put("Subsystems & Capabilities", List.of(IS_VISUAL, IS_OCR_ENABLED, HIGHLIGHT_ELEMENTS, SET_HARD_GATE, IS_FAILING_TEST_SUITE));
        categories.put("Build & Run Info", List.of(BUILD_ID, BUILD_INITIATION_REASON, PARALLEL, TAG));

        Set<String> processedKeys = new HashSet<>();
        for (Map.Entry<String, List<String>> categoryEntry : categories.entrySet()) {
            LOGGER.info("  --- {} ---", categoryEntry.getKey());
            for (String key : categoryEntry.getValue()) {
                if (testRunMetadata.containsKey(key)) {
                    Object val = testRunMetadata.get(key);
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("    %-25s : %s", key, val));
                    }
                    config.addClassifications(key, String.valueOf(val));
                    processedKeys.add(key);
                }
            }
        }

        List<String> remainingKeys = testRunMetadata.keySet().stream()
                .filter(k -> !processedKeys.contains(k))
                .sorted()
                .toList();

        if (!remainingKeys.isEmpty()) {
            LOGGER.info("  --- Additional Session Metadata ---");
            for (String key : remainingKeys) {
                Object val = testRunMetadata.get(key);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info(String.format("    %-25s : %s", key, val));
                }
                config.addClassifications(key, String.valueOf(val));
            }
        }

        return config;
    }

    static HashMap<String, Object> buildTestRunMetadata(String reportsDir) {
        return new HashMap<>(TestExecutionMetadataBuilder.build(reportsDir));
    }
}
