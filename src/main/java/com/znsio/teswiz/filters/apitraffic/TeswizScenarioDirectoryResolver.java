package com.znsio.teswiz.filters.apitraffic;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.runner.Runner;

/**
 * Resolves the current scenario's directory from teswiz's per-scenario report
 * folder ({@code TEST_CONTEXT.SCENARIO_LOG_DIRECTORY}), so api-traffic files land
 * inside {@code reports/<N>-<scenario>_<run>/api-traffic/} alongside the other
 * per-scenario report artifacts.
 * <p>
 * When teswiz's context or scenario directory is unavailable (e.g. a call made
 * outside a running scenario), falls back to
 * {@code <LOG_DIR>/reports/unknown-scenario}.
 */
public class TeswizScenarioDirectoryResolver implements ScenarioDirectoryResolver {

    private static final Logger LOGGER = LogManager.getLogger(TeswizScenarioDirectoryResolver.class.getName());
    private static final String LOG_DIR_PROPERTY = "LOG_DIR";
    private static final String DEFAULT_LOG_DIR = "./target/logs";
    private static final String REPORTS_DIR = "reports";
    private static final String UNKNOWN_SCENARIO_DIR = "unknown-scenario";

    @Override
    public Path resolve() {
        String scenarioDirectory = readScenarioDirectory();
        return scenarioDirectory != null ? Path.of(scenarioDirectory) : fallbackDirectory();
    }

    // --- Private helper methods ---

    private String readScenarioDirectory() {
        try {
            TestExecutionContext context = Runner.getTestExecutionContext(Thread.currentThread().getId());
            if (context == null) {
                return null;
            }
            Object directory = context.getTestState(TEST_CONTEXT.SCENARIO_LOG_DIRECTORY);
            return directory == null ? null : directory.toString();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not read teswiz scenario directory; using fallback: {}", e.getMessage());
            return null;
        }
    }

    private Path fallbackDirectory() {
        String logDir = System.getProperty(LOG_DIR_PROPERTY);
        String resolvedLogDir = logDir != null && !logDir.isBlank() ? logDir : DEFAULT_LOG_DIR;
        return Path.of(resolvedLogDir, REPORTS_DIR, UNKNOWN_SCENARIO_DIR);
    }
}
