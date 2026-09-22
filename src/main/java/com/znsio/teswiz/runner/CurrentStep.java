package com.znsio.teswiz.runner;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CurrentStep {

    private static class StepContext {
        private final AtomicInteger index = new AtomicInteger(0);
        private volatile String text;
    }

    private static final Map<Long, StepContext> THREAD_STEP_MAP = new ConcurrentHashMap<>();

    private CurrentStep() {
    }

    public static void advance(long threadId) {
        StepContext context = THREAD_STEP_MAP.computeIfAbsent(threadId, k -> new StepContext());
        context.index.incrementAndGet();
    }

    public static void setStepText(long threadId, String stepText) {
        StepContext context = THREAD_STEP_MAP.computeIfAbsent(threadId, k -> new StepContext());
        context.text = stepText;
    }

    public static void clearStepText(long threadId) {
        StepContext context = THREAD_STEP_MAP.get(threadId);
        if (context != null) {
            context.text = null;
        }
    }

    public static String describe(long threadId) {
        StepContext context = THREAD_STEP_MAP.get(threadId);
        if (context == null) {
            return null;
        }
        int idx = context.index.get();
        if (idx <= 0) {
            return null;
        }
        String txt = context.text;
        if (txt != null && !txt.isBlank()) {
            return "step " + idx + ": " + txt;
        } else {
            return "step " + idx;
        }
    }

    public static void reset(long threadId) {
        THREAD_STEP_MAP.remove(threadId);
    }

    public static void remove(long threadId) {
        THREAD_STEP_MAP.remove(threadId);
    }
}
