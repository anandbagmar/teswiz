package com.znsio.teswiz.filters.apitraffic;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-scenario, per-thread counter for API calls.
 * <p>
 * Each scenario starts its API call numbering at {@code 01}. Because scenarios
 * can run in parallel (one thread each), the counter is held in a
 * {@link ThreadLocal} so concurrent scenarios never share state.
 * <p>
 * teswiz resets the counter at the start of each scenario and clears it when the
 * scenario finishes (see {@code CucumberScenarioListener}), so consumers never
 * manage this lifecycle themselves.
 */
public final class ApiCallContext {

    private static final ThreadLocal<AtomicInteger> COUNTER = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    private ApiCallContext() {
    }

    /**
     * Resets the call counter so the next call is numbered {@code 01}. Called at
     * the start of each scenario.
     */
    public static void resetForNewScenario() {
        COUNTER.get().set(0);
    }

    /**
     * Returns the next 1-based call index for the current scenario and increments
     * it.
     */
    public static int nextIndex() {
        return COUNTER.get().incrementAndGet();
    }

    /**
     * Clears the counter for the current thread.
     */
    public static void clear() {
        COUNTER.remove();
    }
}
