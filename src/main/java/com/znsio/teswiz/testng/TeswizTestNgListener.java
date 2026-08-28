package com.znsio.teswiz.testng;

import com.znsio.teswiz.steps.Hooks;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.tools.LoggingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class TeswizTestNgListener implements ITestListener {
    private final AtomicInteger runningTestNumber = new AtomicInteger(0);
    private final AtomicInteger passedCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    private final AtomicInteger skippedCount = new AtomicInteger(0);
    private final AtomicInteger startedCount = new AtomicInteger(0);
    private final long startedAtNanos = System.nanoTime();
    private static final Logger LOGGER = LogManager.getLogger(TeswizTestNgListener.class);
    private final Map<String, List<TestOutcome>> outcomesByGroup = new ConcurrentHashMap<>();
    private final List<TestNgScenarioReportData> scenarioReportData = new CopyOnWriteArrayList<>();

    private record TestOutcome(String testName, boolean passed) { }

    @Override
    public void onTestStart(ITestResult result) {
        int testNumber = runningTestNumber.incrementAndGet();
        TestExecutionContext context = TestNgTestExecutionContextFactory.create(result.getName(), testNumber);
        startedCount.incrementAndGet();
        LoggingContext.begin(result.getName(), testNumber, 1,
                context.getTestStateAsString(TEST_CONTEXT.SCENARIO_LOG_DIRECTORY));
        LOGGER.info(String.format("Test started: number=%d, name=\"%s\"", testNumber, result.getName()));
        TestNgStepRecorder.startCapturingStepsForCurrentThread();
        new Hooks().beforeScenario(result.getName());
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        passedCount.incrementAndGet();
        recordOutcomeByGroup(result, true);
        recordScenarioReportData(result, TestNgCapturedStep.PASSED);
        new Hooks().afterScenario(result.getName(), false);
        LOGGER.info(String.format("Test finished: name=\"%s\", status=PASSED", result.getName()));
        LoggingContext.clear();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        failedCount.incrementAndGet();
        recordOutcomeByGroup(result, false);
        recordScenarioReportData(result, TestNgCapturedStep.FAILED);
        new Hooks().afterScenario(result.getName(), true);
        LOGGER.info(String.format("Test finished: name=\"%s\", status=FAILED", result.getName()));
        LoggingContext.clear();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        skippedCount.incrementAndGet();
        recordScenarioReportData(result, TestNgCapturedStep.FAILED);
        new Hooks().afterScenario(result.getName(), true);
        LOGGER.info(String.format("Test finished: name=\"%s\", status=SKIPPED", result.getName()));
        LoggingContext.clear();
    }

    void logExecutionSummary() {
        long durationMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
        LOGGER.info(String.format("Test run completed: total=%d, passed=%d, failed=%d, skipped=%d, durationMs=%d",
                startedCount.get(), passedCount.get(), failedCount.get(), skippedCount.get(), durationMillis));
    }

    private void recordScenarioReportData(ITestResult result, String status) {
        List<TestNgCapturedStep> steps = TestNgStepRecorder.stopCapturingAndGetStepsForCurrentThread();
        String featureName = result.getTestClass().getRealClass().getSimpleName();
        String scenarioName = scenarioNameFor(result);
        List<String> tags = List.of(result.getMethod().getGroups());
        long durationMillis = result.getEndMillis() - result.getStartMillis();
        scenarioReportData.add(new TestNgScenarioReportData(featureName, scenarioName, tags, status, durationMillis, steps));
    }

    private String scenarioNameFor(ITestResult result) {
        Object[] parameters = result.getParameters();
        if (parameters.length == 0) {
            return result.getName();
        }
        String parameterSummary = java.util.Arrays.stream(parameters)
                .map(String::valueOf)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return result.getName() + " [" + parameterSummary + "]";
    }

    List<TestNgScenarioReportData> getScenarioReportData() {
        return List.copyOf(scenarioReportData);
    }

    private void recordOutcomeByGroup(ITestResult result, boolean passed) {
        TestOutcome outcome = new TestOutcome(result.getName(), passed);
        for (String group : result.getMethod().getGroups()) {
            outcomesByGroup.computeIfAbsent(group, g -> new CopyOnWriteArrayList<>()).add(outcome);
        }
    }

    TestNgExecutionResult getExecutionResult() {
        return new TestNgExecutionResult(passedCount.get(), failedCount.get(), buildGroupCoverage());
    }

    private List<TestNgGroupCoverage> buildGroupCoverage() {
        return outcomesByGroup.entrySet().stream()
                .map(entry -> toGroupCoverage(entry.getKey(), entry.getValue()))
                .toList();
    }

    private TestNgGroupCoverage toGroupCoverage(String groupName, List<TestOutcome> outcomes) {
        List<String> passedTestNames = outcomes.stream().filter(TestOutcome::passed).map(TestOutcome::testName).toList();
        List<String> failedTestNames = outcomes.stream().filter(outcome -> !outcome.passed()).map(TestOutcome::testName).toList();
        return new TestNgGroupCoverage(groupName, passedTestNames, failedTestNames);
    }
}
