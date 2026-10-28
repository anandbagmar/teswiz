package com.znsio.teswiz.reporting.notify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The LLM-vision usage for a run: the config-derived context (provider / model / strategy / enabled) combined with the
 * run-level totals teswiz writes in its end-of-run "LLM Vision Usage Summary" block (calls, tokens, duration,
 * captures). teswiz owns this parser because the log format is teswiz's own.
 * <p>
 * {@link #fromRunLog} is deliberately <em>loosely coupled</em> to the log and <em>fail-soft</em>: it scans for the
 * totals by label (not by fixed line position), ignores any line it does not recognise, and — when the log is
 * missing/unreadable or carries no summary block — returns a zero-usage instance (keeping the supplied context) rather
 * than throwing. A format change degrades to "no LLM usage" (hence no LLM content in a notification), never a broken
 * run.
 */
public record LlmUsageLog(LlmContext context, int testsUsingLlm, int totalCalls, int promptTokens,
        int completionTokens, int totalTokens, String durationSeconds, int capturesSaved) {

    private static final Logger LOGGER = LogManager.getLogger(LlmUsageLog.class.getName());

    private static final String SUMMARY_MARKER = "LLM Vision Usage Summary";
    private static final String TESTS_LABEL = "Total tests using LLM vision";
    private static final String CALLS_LABEL = "Total calls";
    private static final String PROMPT_LABEL = "Prompt tokens";
    private static final String COMPLETION_LABEL = "Completion tokens";
    private static final String TOTAL_TOKENS_LABEL = "Total tokens";
    private static final String DURATION_LABEL = "Total duration (sec)";
    private static final String CAPTURES_LABEL = "Captures saved";
    private static final String UNKNOWN_DURATION = "0";

    /**
     * Parses the run-level LLM usage totals from a teswiz run log, combined with the caller-supplied config context.
     * Never throws: a missing/unreadable log or an absent summary block yields a zero-usage instance.
     *
     * @param runLog  the teswiz run log that may contain the "LLM Vision Usage Summary" block
     * @param context the config-derived LLM context (provider/model/strategy/enabled), supplied by the caller
     * @return the parsed usage, or a zero-usage instance carrying {@code context}
     */
    public static LlmUsageLog fromRunLog(Path runLog, LlmContext context) {
        List<String> lines = readLines(runLog);
        if (!containsSummary(lines)) {
            LOGGER.debug("No LLM Vision Usage Summary found in {}; treating as zero usage.", runLog);
            return empty(context);
        }
        return new LlmUsageLog(context, intAfterLabel(lines, TESTS_LABEL), intAfterLabel(lines, CALLS_LABEL),
                intAfterLabel(lines, PROMPT_LABEL), intAfterLabel(lines, COMPLETION_LABEL),
                intAfterLabel(lines, TOTAL_TOKENS_LABEL), textAfterLabel(lines, DURATION_LABEL, UNKNOWN_DURATION),
                intAfterLabel(lines, CAPTURES_LABEL));
    }

    /**
     * @param context the config-derived LLM context to carry
     * @return a zero-usage instance that still carries the supplied context
     */
    public static LlmUsageLog empty(LlmContext context) {
        return new LlmUsageLog(context, 0, 0, 0, 0, 0, UNKNOWN_DURATION, 0);
    }

    /**
     * @return {@code true} when at least one LLM-vision call was made (the gate for including LLM content)
     */
    public boolean hasCalls() {
        return totalCalls > 0;
    }

    /**
     * @return whether LLM vision was enabled for the run (from config context)
     */
    public boolean enabled() {
        return context.enabled();
    }

    /**
     * @return the active vision strategy (from config context)
     */
    public String strategy() {
        return context.strategy();
    }

    /**
     * @return the LLM provider (from config context)
     */
    public String provider() {
        return context.provider();
    }

    /**
     * @return the LLM model/deployment (from config context)
     */
    public String model() {
        return context.model();
    }

    private static List<String> readLines(Path runLog) {
        if (runLog == null || !Files.exists(runLog)) {
            return List.of();
        }
        try {
            return Files.readAllLines(runLog);
        } catch (IOException e) {
            LOGGER.debug("Could not read LLM run log {}; treating as zero usage: {}", runLog, e.getMessage());
            return List.of();
        }
    }

    private static boolean containsSummary(List<String> lines) {
        return lines.stream().anyMatch(line -> line.contains(SUMMARY_MARKER));
    }

    private static int intAfterLabel(List<String> lines, String label) {
        String value = textAfterLabel(lines, label, "");
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String textAfterLabel(List<String> lines, String label, String fallback) {
        for (String line : lines) {
            int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).contains(label)) {
                String value = line.substring(colon + 1).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return fallback;
    }

    /**
     * The config-derived LLM context supplied by the caller (read from the consumer's loaded config, never re-read
     * from a file by this parser). Kept separate from the run totals so the parser stays decoupled from config loading.
     *
     * @param enabled  whether LLM vision was enabled
     * @param strategy the active vision strategy
     * @param provider the LLM provider
     * @param model    the LLM model/deployment
     */
    public record LlmContext(boolean enabled, String strategy, String provider, String model) {
    }
}
