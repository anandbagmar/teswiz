package com.znsio.teswiz.tools;

import org.apache.logging.log4j.ThreadContext;

public final class LoggingContext {
    private LoggingContext() {
    }

    public static void begin(String scenarioName, int scenarioNumber, int exampleRow, String scenarioLogDirectory) {
        ThreadContext.put("scenario", scenarioName);
        ThreadContext.put("scenarioNumber", String.valueOf(scenarioNumber));
        ThreadContext.put("exampleRow", String.valueOf(exampleRow));
        ThreadContext.put("scenarioLogDirectory", scenarioLogDirectory);
    }

    public static void clear() {
        ThreadContext.remove("scenario");
        ThreadContext.remove("scenarioNumber");
        ThreadContext.remove("exampleRow");
        ThreadContext.remove("scenarioLogDirectory");
    }
}
