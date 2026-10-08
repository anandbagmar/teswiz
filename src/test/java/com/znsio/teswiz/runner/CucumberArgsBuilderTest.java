package com.znsio.teswiz.runner;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CucumberArgsBuilder}, extracted from {@link Setup#addCucumberPlugsToArgs()}.
 * Locks the plugin ordering and the report file paths derived from the log directory.
 */
class CucumberArgsBuilderTest {

    @Test
    void buildsPluginArgsInExpectedOrderWithReportPaths() {
        String logDir = "/tmp/logs";
        String base = logDir + File.separator + "reports" + File.separator;

        List<String> args = CucumberArgsBuilder.buildReportingPluginArgs(logDir);

        assertThat(args).containsExactly(
                "--plugin", "pretty",
                "--plugin", "html:" + base + "cucumber-html-report.html",
                "--plugin", "junit:" + base + "cucumber-junit-report.xml",
                "--plugin", "json:" + base + "cucumber-json-report.json",
                "--plugin", "message:" + base + "results.ndjson",
                "--plugin", "timeline:" + base + "timeline");
    }

    @Test
    void setsCucumberPublishQuietSystemProperty() {
        System.clearProperty("cucumber.publish.quiet");

        CucumberArgsBuilder.buildReportingPluginArgs("/tmp/logs");

        assertThat(System.getProperty("cucumber.publish.quiet")).isEqualTo("true");
    }
}
