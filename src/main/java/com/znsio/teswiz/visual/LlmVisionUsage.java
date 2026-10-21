package com.znsio.teswiz.visual;

/**
 * Mutable, thread-confined accumulator of LLM vision token usage for a single test (scenario).
 *
 * <p>One instance is stored per test in that test's {@code TestExecutionContext} (under
 * {@code TEST_CONTEXT.LLM_VISION_USAGE}) and mutated only from the owning test thread, so it needs
 * no internal synchronisation. At scenario finish a snapshot is committed to the process-wide
 * {@link LlmVisionUsageRegistry} for the end-of-run per-test breakdown.
 */
public class LlmVisionUsage {

    private int calls;
    private long promptTokens;
    private long completionTokens;
    private long totalTokens;
    private long totalDurationMs;
    private final java.util.List<Capture> captures = new java.util.ArrayList<>();

    /**
     * A harvested LLM vision capture: the located element cropped to disk, plus the suggested
     * image-template file name for manual promotion into the user's library.
     */
    public record Capture(String description, String savedPath, String suggestedFileName) {
    }

    /**
     * Records a single LLM vision request's token usage and wall-clock duration. Missing counts
     * are passed as {@code 0}.
     */
    public void record(long promptTokens, long completionTokens, long totalTokens, long durationMs) {
        this.calls++;
        this.promptTokens += Math.max(0, promptTokens);
        this.completionTokens += Math.max(0, completionTokens);
        this.totalTokens += Math.max(0, totalTokens);
        this.totalDurationMs += Math.max(0, durationMs);
    }

    public int getCalls() {
        return calls;
    }

    public long getPromptTokens() {
        return promptTokens;
    }

    public long getCompletionTokens() {
        return completionTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public double getTotalDurationSeconds() {
        return totalDurationMs / 1000.0;
    }

    /**
     * Records a harvested capture (a located element saved as a reusable template crop).
     */
    public void recordCapture(String description, String savedPath, String suggestedFileName) {
        this.captures.add(new Capture(description, savedPath, suggestedFileName));
    }

    public int getCaptureCount() {
        return captures.size();
    }

    public java.util.List<Capture> getCaptures() {
        return java.util.Collections.unmodifiableList(captures);
    }

    public boolean hasActivity() {
        return calls > 0 || !captures.isEmpty();
    }

    /**
     * Returns an immutable snapshot copy suitable for storing in the run-level registry.
     */
    public LlmVisionUsage snapshot() {
        LlmVisionUsage copy = new LlmVisionUsage();
        copy.calls = this.calls;
        copy.promptTokens = this.promptTokens;
        copy.completionTokens = this.completionTokens;
        copy.totalTokens = this.totalTokens;
        copy.totalDurationMs = this.totalDurationMs;
        copy.captures.addAll(this.captures);
        return copy;
    }

    @Override
    public String toString() {
        return String.format("%d call(s), tokens[prompt=%d, completion=%d, total=%d], durationSeconds=%.2f",
                calls, promptTokens, completionTokens, totalTokens, getTotalDurationSeconds());
    }
}
