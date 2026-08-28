package com.znsio.teswiz.listener;

import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.runner.AppiumServerManager;
import com.znsio.teswiz.runner.FileLocations;
import com.znsio.teswiz.config.browser.PlaywrightBrowserConfigMigrationReporter;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.tools.FileUtils;
import com.znsio.teswiz.tools.OsUtils;
import com.znsio.teswiz.tools.StringUtils;
import com.znsio.teswiz.tools.LoggingContext;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

public class CucumberScenarioListener implements ConcurrentEventListener {
    private static final Logger LOGGER = Logger.getLogger(CucumberScenarioListener.class.getName());
    private final Map<String, AtomicInteger> numberOfExamplesForScenario = new ConcurrentHashMap<>();
    private final AtomicInteger runningScenarioNumber = new AtomicInteger();
    private final AtomicInteger passedCount = new AtomicInteger();
    private final AtomicInteger failedCount = new AtomicInteger();
    private final AtomicInteger skippedCount = new AtomicInteger();
    private volatile long runStartedNanos;

    public CucumberScenarioListener() {
        LOGGER.info(String.format("ThreadID: %d: CucumberScenarioListener%n", Thread.currentThread().getId()));
        setLog4jCompatibility();
        FileUtils.createDirectoryIn(OsUtils.getUserDirectory(), FileLocations.OUTPUT_DIRECTORY);
    }

    private void setLog4jCompatibility() {
        // Migrating from Log4j 1.x to 2.x - https://logging.apache.org/log4j/2.x/manual/migration.html
        System.setProperty("log4j1.compatibility", "true");
    }

    @Override
    public void setEventPublisher(EventPublisher eventPublisher) {
        eventPublisher.registerHandlerFor(TestRunStarted.class, this::runStartedHandler);
        eventPublisher.registerHandlerFor(TestCaseStarted.class, this::scenarioStartedHandler);
        eventPublisher.registerHandlerFor(TestCaseFinished.class, this::scenarioFinishedHandler);
        eventPublisher.registerHandlerFor(TestRunFinished.class, this::runFinishedHandler);
    }

    private void runStartedHandler(TestRunStarted event) {
        runStartedNanos = System.nanoTime();
        LOGGER.info("Test run started");
    }

    private void scenarioStartedHandler(TestCaseStarted event) {
        String scenarioName = event.getTestCase().getName();
        int currentExampleRowNumberForScenario = updateCurrentExampleRowNumberForScenario(scenarioName);
        int scenarioNumber = runningScenarioNumber.incrementAndGet();
        TestExecutionContext testExecutionContext = new TestExecutionContext(scenarioName + "-" + currentExampleRowNumberForScenario);

        String normalisedScenarioName = StringUtils.normaliseScenarioName(scenarioName);
        String scenarioLogDirectory = FileLocations.REPORTS_DIRECTORY + scenarioNumber + "-" + normalisedScenarioName + "_" + currentExampleRowNumberForScenario + File.separator;
        String screenshotDirectory = scenarioLogDirectory + FileLocations.SCREENSHOTS_DIRECTORY;
        String deviceLogsDirectory = scenarioLogDirectory + FileLocations.DEVICE_LOGS_DIRECTORY;

        scenarioLogDirectory = FileUtils.createDirectoryIn(OsUtils.getUserDirectory(), scenarioLogDirectory).getAbsolutePath();
        screenshotDirectory = FileUtils.createDirectoryIn(OsUtils.getUserDirectory(), screenshotDirectory).getAbsolutePath();
        deviceLogsDirectory = FileUtils.createDirectoryIn(OsUtils.getUserDirectory(), deviceLogsDirectory).getAbsolutePath();
        testExecutionContext.addTestState(TEST_CONTEXT.EXAMPLE_RUN_COUNT, currentExampleRowNumberForScenario);
        testExecutionContext.addTestState(TEST_CONTEXT.SCENARIO_RUN_COUNT, scenarioNumber);
        testExecutionContext.addTestState(TEST_CONTEXT.NORMALISED_SCENARIO_NAME, normalisedScenarioName);
        testExecutionContext.addTestState(TEST_CONTEXT.SCENARIO_LOG_DIRECTORY, scenarioLogDirectory);
        testExecutionContext.addTestState(TEST_CONTEXT.SCREENSHOT_DIRECTORY, screenshotDirectory);
        testExecutionContext.addTestState(TEST_CONTEXT.DEVICE_LOGS_DIRECTORY, deviceLogsDirectory);
        com.znsio.teswiz.filters.apitraffic.ApiCallContext.resetForNewScenario();
        LoggingContext.begin(scenarioName, scenarioNumber, currentExampleRowNumberForScenario, scenarioLogDirectory);
        LOGGER.info(String.format("Scenario started: number=%d, name=\"%s\", exampleRow=%d",
                scenarioNumber, scenarioName, currentExampleRowNumberForScenario));
    }

    private int updateCurrentExampleRowNumberForScenario(String scenarioName) {
        return numberOfExamplesForScenario.computeIfAbsent(scenarioName, ignored -> new AtomicInteger()).incrementAndGet();
    }

    private Integer getCurrentExampleRowNumberForScenario(String scenarioName) {
        AtomicInteger row = numberOfExamplesForScenario.get(scenarioName);
        return row == null ? 0 : row.get();
    }

    private void scenarioFinishedHandler(TestCaseFinished event) {
        String scenarioName = event.getTestCase().getName();
        Integer currentExampleRowNumberForScenario = getCurrentExampleRowNumberForScenario(scenarioName);

        Status status = event.getResult().getStatus();
        if (status == Status.PASSED) passedCount.incrementAndGet();
        else if (status == Status.FAILED || status == Status.AMBIGUOUS) failedCount.incrementAndGet();
        else skippedCount.incrementAndGet();
        LOGGER.info(String.format("Scenario finished: name=\"%s\", exampleRow=%d, status=%s",
                scenarioName, currentExampleRowNumberForScenario, status));

        long threadId = Thread.currentThread().getId();
        TestExecutionContext testExecutionContext = SessionContext.getTestExecutionContext(threadId);

        com.znsio.teswiz.filters.apitraffic.ApiCallContext.clear();
        SessionContext.remove(threadId);
        LoggingContext.clear();
    }

    private void runFinishedHandler(TestRunFinished event) {
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - runStartedNanos);
        LOGGER.info(String.format("Test run completed: total=%d, passed=%d, failed=%d, skipped=%d, durationMs=%d",
                runningScenarioNumber.get(), passedCount.get(), failedCount.get(), skippedCount.get(), durationMillis));
        try {
            Visual.closeBatch();
            AppiumServerManager.destroyAppiumNode();
            PlaywrightBrowserConfigMigrationReporter.emitSummaryIfPresent();
            SessionContext.setReportPortalLaunchURL();
        } catch (Exception e) {
            ExceptionUtils.getStackTrace(e);
        }
    }
}
