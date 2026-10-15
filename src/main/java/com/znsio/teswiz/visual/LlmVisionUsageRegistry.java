package com.znsio.teswiz.visual;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Process-wide, thread-safe registry of per-test LLM vision token usage, used to print the
 * end-of-run summary (per-test breakdown plus totals).
 *
 * <p>Per-test accumulation happens in each test's thread-confined {@link LlmVisionUsage}; at
 * scenario finish a snapshot is committed here keyed by a unique per-run test key (scenario name +
 * run count) so parallel, duplicate, or retried scenarios do not clobber one another.
 */
public final class LlmVisionUsageRegistry {

    private static final Logger LOGGER = LogManager.getLogger(LlmVisionUsageRegistry.class.getName());

    // Preserves commit order for a readable summary.
    private static final Map<String, LlmVisionUsage> USAGE_BY_TEST = new ConcurrentHashMap<>();
    private static final AtomicLong COMMIT_SEQUENCE = new AtomicLong();
    private static final Map<String, Long> ORDER_BY_TEST = new ConcurrentHashMap<>();

    private LlmVisionUsageRegistry() {
    }

    /**
     * Commits a test's final usage snapshot. No-op when the usage is null or had no activity.
     *
     * @param testKey a unique-per-run key for the test (e.g. scenario name + run count)
     * @param usage   the test's accumulated usage
     */
    public static void commit(String testKey, LlmVisionUsage usage) {
        if (testKey == null || usage == null || !usage.hasActivity()) {
            return;
        }
        USAGE_BY_TEST.put(testKey, usage.snapshot());
        ORDER_BY_TEST.putIfAbsent(testKey, COMMIT_SEQUENCE.incrementAndGet());
    }

    /**
     * Logs the per-test LLM vision usage summary and commits the test's snapshot to the registry.
     * Shared by the Cucumber and TestNG runners (invoked from {@code Hooks.afterScenario}). No-op
     * when the context is null or the test made no LLM vision calls.
     */
    public static void summariseAndCommit(com.znsio.teswiz.context.TestExecutionContext testExecutionContext) {
        if (testExecutionContext == null) {
            return;
        }
        Object usageState = testExecutionContext.getTestState(com.znsio.teswiz.entities.TEST_CONTEXT.LLM_VISION_USAGE);
        if (!(usageState instanceof LlmVisionUsage usage) || !usage.hasActivity()) {
            return;
        }
        String testKey = buildTestKey(testExecutionContext);
        StringBuilder perTest = new StringBuilder();
        perTest.append(String.format("%n----- LLM Vision Usage (test: %s) -----%n"
                        + " Calls          : %d%n"
                        + " Prompt tokens  : %d%n"
                        + " Completion     : %d%n"
                        + " Total tokens   : %d%n"
                        + " Duration (sec) : %.2f%n",
                testKey, usage.getCalls(), usage.getPromptTokens(), usage.getCompletionTokens(),
                usage.getTotalTokens(), usage.getTotalDurationSeconds()));
        if (usage.getCaptureCount() > 0) {
            perTest.append(String.format(" Captures saved : %d (suggestions for your image library)%n", usage.getCaptureCount()));
            for (LlmVisionUsage.Capture c : usage.getCaptures()) {
                perTest.append(String.format("   - %s -> %s (suggested name: %s)%n",
                        c.description(), c.savedPath(), c.suggestedFileName()));
            }
        }
        perTest.append("---------------------------------------");
        LOGGER.info(perTest.toString());
        commit(testKey, usage);
    }

    private static String buildTestKey(com.znsio.teswiz.context.TestExecutionContext testExecutionContext) {
        Object normalisedName = testExecutionContext.getTestState(com.znsio.teswiz.entities.TEST_CONTEXT.NORMALISED_SCENARIO_NAME);
        Object runCount = testExecutionContext.getTestState(com.znsio.teswiz.entities.TEST_CONTEXT.EXAMPLE_RUN_COUNT);
        String name = normalisedName != null ? normalisedName.toString() : testExecutionContext.getTestName();
        return runCount != null ? name + "_" + runCount : name;
    }

    public static boolean hasAnyActivity() {
        return !USAGE_BY_TEST.isEmpty();
    }

    /**
     * Logs the end-of-run summary: a per-test breakdown followed by run totals. Silent when no test
     * used the LLM vision fallback.
     */
    public static void logRunSummary() {
        if (!hasAnyActivity()) {
            return;
        }
        StringBuilder summary = new StringBuilder();
        summary.append(System.lineSeparator())
                .append("===================== LLM Vision Usage Summary =====================").append(System.lineSeparator())
                .append(" Per test:").append(System.lineSeparator());

        RunTotals totals = getRunTotals();
        for (Map.Entry<String, LlmVisionUsage> entry : sortedByCommitOrder()) {
            LlmVisionUsage usage = entry.getValue();
            summary.append(String.format("   %-50s %d call(s), %d tokens, %.2f sec, %d capture(s)%n",
                    truncate(entry.getKey(), 50), usage.getCalls(), usage.getTotalTokens(),
                    usage.getTotalDurationSeconds(), usage.getCaptureCount()));
        }

        summary.append(" ------------------------------------------------------------------").append(System.lineSeparator())
                .append(String.format(" Total tests using LLM vision : %d%n", totals.testsUsingLlm()))
                .append(String.format(" Total calls                  : %d%n", totals.totalCalls()))
                .append(String.format(" Prompt tokens                : %d%n", totals.promptTokens()))
                .append(String.format(" Completion tokens            : %d%n", totals.completionTokens()))
                .append(String.format(" Total tokens                 : %d%n", totals.totalTokens()))
                .append(String.format(" Total duration (sec)         : %.2f%n", totals.totalDurationSeconds()))
                .append(String.format(" Captures saved               : %d%n", totals.totalCaptures()));

        appendCapturesSection(summary);
        summary.append("====================================================================");
        LOGGER.info(summary.toString());
    }

    /**
     * Appends the harvested-captures listing (suggestions for manual promotion into the user's
     * image-template library). Silent when no captures were saved this run.
     */
    private static void appendCapturesSection(StringBuilder summary) {
        if (getRunTotals().totalCaptures() == 0) {
            return;
        }
        summary.append(System.lineSeparator())
                .append(" New LLM vision captures (review, then copy into your image library to skip the LLM next time):")
                .append(System.lineSeparator());
        for (Map.Entry<String, LlmVisionUsage> entry : sortedByCommitOrder()) {
            for (LlmVisionUsage.Capture c : entry.getValue().getCaptures()) {
                summary.append(String.format("   - %s%n       saved: %s%n       suggested name: %s%n",
                        c.description(), c.savedPath(), c.suggestedFileName()));
            }
        }
    }

    /**
     * Immutable, privacy-safe aggregate of LLM vision usage across the run, for telemetry.
     */
    public record RunTotals(int testsUsingLlm, long totalCalls, long promptTokens, long completionTokens,
                            long totalTokens, long totalDurationMs, long totalCaptures) {
        public double totalDurationSeconds() {
            return totalDurationMs / 1000.0;
        }
    }

    /**
     * Returns the aggregate run totals (numeric only; no test names, descriptions, or identifiers).
     */
    public static RunTotals getRunTotals() {
        long totalCalls = 0;
        long totalPrompt = 0;
        long totalCompletion = 0;
        long totalTokens = 0;
        long totalDurationMs = 0;
        long totalCaptures = 0;
        for (LlmVisionUsage usage : USAGE_BY_TEST.values()) {
            totalCalls += usage.getCalls();
            totalPrompt += usage.getPromptTokens();
            totalCompletion += usage.getCompletionTokens();
            totalTokens += usage.getTotalTokens();
            totalDurationMs += usage.getTotalDurationMs();
            totalCaptures += usage.getCaptureCount();
        }
        return new RunTotals(USAGE_BY_TEST.size(), totalCalls, totalPrompt, totalCompletion, totalTokens, totalDurationMs, totalCaptures);
    }

    private static Iterable<Map.Entry<String, LlmVisionUsage>> sortedByCommitOrder() {
        return USAGE_BY_TEST.entrySet().stream()
                .sorted((a, b) -> Long.compare(
                        ORDER_BY_TEST.getOrDefault(a.getKey(), Long.MAX_VALUE),
                        ORDER_BY_TEST.getOrDefault(b.getKey(), Long.MAX_VALUE)))
                .toList();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 3) + "...";
    }

    /**
     * Clears all recorded usage. Intended for test isolation.
     */
    public static void reset() {
        USAGE_BY_TEST.clear();
        ORDER_BY_TEST.clear();
        COMMIT_SEQUENCE.set(0);
    }
}
