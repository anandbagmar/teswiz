package com.znsio.teswiz.reporting.notify.failure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.reporting.notify.JUnitResults.FeatureScenarios;
import com.znsio.teswiz.reporting.notify.JUnitResults.Scenario;
import com.znsio.teswiz.reporting.notify.JUnitResults.ScenarioStatus;

/**
 * One renderer produces the failure breakdown for both the Slack message and the HTML email, so the block looks the
 * same wherever it appears. These cases pin that output: the heading carries the total, each cause shows its simple
 * type name, its place and its count, biggest first, and an empty breakdown renders nothing so a green run is
 * untouched.
 */
class FailureBreakdownRendererTest {

    private final FailureBreakdown breakdown = FailureBreakdown.of(List.of(new FeatureScenarios("F", List.of(
            failedAt("a", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
            failedAt("b", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
            failedAt("c", "com.x.PoolExhaustedException", "UserPool", "lease", 9),
            new Scenario("d", ScenarioStatus.FAILED, "no type", 0.1)))));

    @Test
    @DisplayName("slack heads with the total, then one line per cause with its place and count, biggest first")
    void slackBlock() {
        String slack = FailureBreakdownRenderer.slack(breakdown);

        assertThat(slack).startsWith("*Why did things fail? (4)*\n");
        assertThat(slack).contains("  IndexOutOfBoundsException @ RewardsBL.awardFreeSpins:113  2\n");
        assertThat(slack).contains("  PoolExhaustedException @ UserPool.lease:9  1\n");
        assertThat(slack).contains("  Unclassified  1\n");
        assertThat(slack.indexOf("IndexOutOfBoundsException")).isLessThan(slack.indexOf("PoolExhaustedException"));
        assertThat(slack).doesNotContain("java.lang").doesNotContain("com.x");
    }

    @Test
    @DisplayName("html is a table of type, place and count with a total row")
    void htmlBlock() {
        String html = FailureBreakdownRenderer.html(breakdown);

        assertThat(html).contains("<h3>Failure breakdown</h3>");
        assertThat(html).contains("<tr><th>Exception type</th><th>Where</th><th>Count</th></tr>");
        assertThat(html).contains(
                "<tr><td>IndexOutOfBoundsException</td><td>RewardsBL.awardFreeSpins:113</td><td>2</td></tr>");
        assertThat(html).contains("<tr><td>Unclassified</td><td></td><td>1</td></tr>");
        assertThat(html).contains("<tr><th colspan=\"2\">Total failed</th><td>4</td></tr>");
        assertThat(html).doesNotContain("java.lang").doesNotContain("com.x");
    }

    @Test
    @DisplayName("html escapes a place containing angle brackets, such as a constructor frame")
    void htmlEscapesThePlace() {
        FailureBreakdown withConstructor = FailureBreakdown.of(List.of(new FeatureScenarios("F", List.of(
                new Scenario("a", ScenarioStatus.FAILED, "x", 0.1, "com.x.BoomError",
                        "com.x.BoomError: x\n\tat com.app.Widget.<init>(Widget.java:7)\n")))));

        String html = FailureBreakdownRenderer.html(withConstructor);

        assertThat(html).contains("<td>Widget.&lt;init&gt;:7</td>").doesNotContain("<init>");
    }

    @Test
    @DisplayName("an empty breakdown renders nothing for both channels")
    void emptyRendersNothing() {
        assertThat(FailureBreakdownRenderer.slack(FailureBreakdown.empty())).isEmpty();
        assertThat(FailureBreakdownRenderer.html(FailureBreakdown.empty())).isEmpty();
    }

    private static Scenario failedAt(String name, String type, String className, String method, int line) {
        String trace = type + ": x\n\tat com.app." + className + "." + method + "(" + className + ".java:" + line
                + ")\n";
        return new Scenario(name, ScenarioStatus.FAILED, "x", 0.1, type, trace);
    }
}
