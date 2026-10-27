package com.znsio.teswiz.reporting.notify;

import java.nio.file.Path;
import java.util.Map;

/**
 * The facts teswiz hands a {@link RunNotificationComposer} at the end of a run — everything teswiz already knows at the
 * point the cucumber report is complete and before the JVM exits. The composer uses these (plus whatever it reads from
 * the environment itself) to build the message content.
 *
 * @param junitReportXml   the path to the completed {@code cucumber-junit-report.xml} (may be {@code null} when no
 *                         scenarios ran); parse it with {@link JUnitResults}
 * @param logDir           the run's log directory (for artifacts such as the LLM-vision usage log)
 * @param totalScenarios   the total scenarios executed
 * @param passedScenarios  the passed scenarios
 * @param failedScenarios  the failed scenarios
 * @param environment      the process environment ({@code System.getenv()}), so the composer can resolve its own
 *                         consumer-specific inputs (branch, CI URLs, domain identity)
 * @param runEnvironment   the resolved CI/local run provenance (for the footer and branch)
 */
public record RunResult(Path junitReportXml, Path logDir, int totalScenarios, int passedScenarios, int failedScenarios,
        Map<String, String> environment, RunEnvironment runEnvironment) {

    /**
     * Canonical constructor; defensively copies the environment map.
     */
    public RunResult {
        environment = environment == null ? Map.of() : Map.copyOf(environment);
    }

    /**
     * @return {@code true} when any scenario failed
     */
    public boolean hasFailures() {
        return failedScenarios > 0;
    }
}
