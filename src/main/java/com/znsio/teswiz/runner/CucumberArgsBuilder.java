package com.znsio.teswiz.runner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the Cucumber reporting {@code --plugin} arguments (pretty + html/junit/json/message/timeline
 * report paths) for a given log directory.
 *
 * <p>Extracted from {@link Setup#addCucumberPlugsToArgs()} so the plugin wiring lives in one focused,
 * unit-testable place. The produced argument order is preserved exactly.
 */
public final class CucumberArgsBuilder {

    private static final String PLUGIN = Setup.PLUGIN;
    private static final String REPORTS_DIR = Setup.REPORTS_DIR;

    private CucumberArgsBuilder() {
    }

    /**
     * Returns the ordered list of Cucumber reporting plugin arguments for the given log directory,
     * and sets {@code cucumber.publish.quiet=true} as the original inline code did.
     */
    public static List<String> buildReportingPluginArgs(String logDir) {
        List<String> args = new ArrayList<>();
        args.add(PLUGIN);
        args.add("pretty");
        args.add(PLUGIN);
        args.add("html:" + reportPath(logDir, "cucumber-html" + "-report.html"));
        args.add(PLUGIN);
        args.add("junit:" + reportPath(logDir, "cucumber" + "-junit-report.xml"));
        args.add(PLUGIN);
        args.add("json:" + reportPath(logDir, "cucumber-json" + "-report.json"));
        args.add(PLUGIN);
        args.add("message:" + reportPath(logDir, "results" + ".ndjson"));
        args.add(PLUGIN);
        args.add("timeline:" + reportPath(logDir, "timeline"));
        System.setProperty("cucumber.publish.quiet", "true");
        return args;
    }

    private static String reportPath(String logDir, String fileName) {
        return logDir + File.separator + REPORTS_DIR + File.separator + fileName;
    }
}
