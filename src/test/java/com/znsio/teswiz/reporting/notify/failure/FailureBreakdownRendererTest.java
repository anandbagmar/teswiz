package com.znsio.teswiz.reporting.notify.failure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.reporting.notify.JUnitResults.FeatureScenarios;
import com.znsio.teswiz.reporting.notify.JUnitResults.Scenario;
import com.znsio.teswiz.reporting.notify.JUnitResults.ScenarioStatus;

/**
 * One renderer produces the failure breakdown for both the Slack message and the HTML email. These cases pin the HTML
 * table — the friendly-label "Failure" column, the "Where" column carrying the code location with the raw class name
 * beside it, the failing-step column, grouping by type and location, the total row and escaping — and that an empty
 * breakdown renders nothing in either channel, so a green run is untouched. The Slack layout is pinned by
 * {@link FailureBreakdownSlackLayoutTest}.
 */
class FailureBreakdownRendererTest {

    // A consumer's friendly label: name PoolExhaustedException, let everything else fall back to its simple class name.
    private static final Function<String, String> FRIENDLY = type -> "PoolExhaustedException".equals(type)
            ? "No test user available"
            : type;

    @Test
    @DisplayName("html groups by type and location, showing the label, the location with the raw class, and the count")
    void htmlGroupsWithLabelAndRawClass() {
        FailureBreakdown breakdown = FailureBreakdown.of(List.of(new FeatureScenarios("F",
                List.of(failedAt("a", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                        failedAt("b", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                        failedAt("c", "com.x.PoolExhaustedException", "UserPool", "lease", 9)))));

        String html = FailureBreakdownRenderer.html(breakdown, FRIENDLY);

        assertThat(html).contains("<h3>Failure breakdown</h3>");
        assertThat(html).contains("<table class=\"breakdown\">");
        assertThat(html)
                .contains("<tr><th>Failure</th><th>Where</th><th>Failing step</th><th class=\"count\">Count</th></tr>");
        assertThat(html).contains("<tr><td>IndexOutOfBoundsException</td><td>RewardsBL.awardFreeSpins:113 "
                + "(IndexOutOfBoundsException)</td><td></td><td class=\"count\">\u00d72</td></tr>");
        assertThat(html)
                .contains("<tr><td>No test user available</td><td>UserPool.lease:9 (PoolExhaustedException)</td>"
                        + "<td></td><td class=\"count\">\u00d71</td></tr>");
        assertThat(html).contains("<tr><th colspan=\"3\">Total failed</th><td class=\"count\">3</td></tr>");
        assertThat(html).doesNotContain("java.lang").doesNotContain("com.x");
    }

    @Test
    @DisplayName("one location reached by several steps becomes one group with a nested row per step")
    void htmlNestsStepsUnderAGroup() {
        FailureBreakdown breakdown = FailureBreakdown
                .of(List.of(new FeatureScenarios("F", List.of(softFailure("a", "reduced by 1.0"),
                        softFailure("b", "reduced by 1.0"), softFailure("c", "increased by 5.0")))));

        String html = FailureBreakdownRenderer.html(breakdown);

        // One group header (subtotal 3) at the shared location, then a nested row per distinct step with its count.
        assertThat(html).contains("<tr><td>AssertJMultipleFailuresError</td><td>BalanceTracking.assertDelta:182 "
                + "(AssertJMultipleFailuresError)</td><td></td><td class=\"count\">\u00d73</td></tr>");
        assertThat(html).contains(
                "<tr><td></td><td></td><td>Then the balance is reduced by 1.0</td><td class=\"count\">\u00d72</td></tr>");
        assertThat(html).contains("<tr><td></td><td></td><td>Then the balance is increased by 5.0</td>"
                + "<td class=\"count\">\u00d71</td></tr>");
        assertThat(html).contains("<tr><th colspan=\"3\">Total failed</th><td class=\"count\">3</td></tr>");
    }

    @Test
    @DisplayName("html escapes a place containing angle brackets, such as a constructor frame")
    void htmlEscapesThePlace() {
        FailureBreakdown withConstructor = FailureBreakdown
                .of(List.of(new FeatureScenarios("F", List.of(new Scenario("a", ScenarioStatus.FAILED, "x", 0.1,
                        "com.x.BoomError", "com.x.BoomError: x\n\tat com.app.Widget.<init>(Widget.java:7)\n")))));

        String html = FailureBreakdownRenderer.html(withConstructor);

        assertThat(html).contains("Widget.&lt;init&gt;:7").doesNotContain("<init>");
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

    // A soft-assertion failure at BalanceTracking.assertDelta:182 on the given step (as a "Then the balance is ..."),
    // shaped like the real JUnit message so the classifier reads both the location and the step from it.
    private static Scenario softFailure(String name, String what) {
        String message = "\nMultiple Failures (1 failure)\n-- failure 1 --\n[step 4: Then the balance is " + what
                + "] [desc]\nat BalanceTracking.assertDelta(BalanceTracking.java:182)";
        return new Scenario(name, ScenarioStatus.FAILED, message, 0.1,
                "org.assertj.core.error.AssertJMultipleFailuresError", message);
    }
}
