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

    // A consumer's friendly label, composed the way a real consumer does: override PoolExhaustedException explicitly,
    // and fall back to the teswiz humaniser for everything else (so IndexOutOfBoundsException reads "Index out of
    // bounds", not the raw class name).
    private static final Function<String, String> FRIENDLY = type -> "PoolExhaustedException".equals(type)
            ? "No test user available"
            : FailureBreakdownRenderer.HUMANIZED_TYPE.apply(type);

    @Test
    @DisplayName("html groups by type and location, showing the label, the location above the raw class, and the count")
    void htmlGroupsWithLabelAndRawClass() {
        FailureBreakdown breakdown = FailureBreakdown.of(List.of(new FeatureScenarios("F",
                List.of(failedAt("a", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                        failedAt("b", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                        failedAt("c", "com.x.PoolExhaustedException", "UserPool", "lease", 9)))));

        String html = FailureBreakdownRenderer.html(breakdown, FRIENDLY);

        assertThat(html).contains("<h3>Failure breakdown</h3>");
        assertThat(html).contains("<table class=\"breakdown\"");
        // Percentage column widths in a colgroup (not pixels) so the table fills the card and scales with the
        // viewport in every client, including Outlook which ignores <style>-block widths.
        assertThat(html).contains("<colgroup>");
        assertThat(html)
                .contains("<tr><th>Failure</th><th>Where</th><th>Failing step</th><th class=\"count\">Count</th></tr>");
        // The "Where" cell carries the location on the first line and the raw class on the second (a <br>), with no
        // parentheses. The "Failure" column shows the friendly label, humanised when the consumer does not map it.
        assertThat(html).contains("<tr><td>Index out of bounds</td>"
                + "<td>RewardsBL.awardFreeSpins:113<br>IndexOutOfBoundsException</td>"
                + "<td></td><td class=\"count\">\u00d72</td></tr>");
        assertThat(html)
                .contains("<tr><td>No test user available</td>" + "<td>UserPool.lease:9<br>PoolExhaustedException</td>"
                        + "<td></td><td class=\"count\">\u00d71</td></tr>");
        assertThat(html).contains("<tr><th colspan=\"3\">Total failed</th><td class=\"count\">3</td></tr>");
        assertThat(html).doesNotContain("java.lang").doesNotContain("com.x");
        assertThat(html).doesNotContain("(IndexOutOfBoundsException)").doesNotContain("(PoolExhaustedException)");
    }

    @Test
    @DisplayName("the breakdown table is full-width and fixed-layout inline, so Outlook renders it responsively")
    void htmlTableIsResponsiveInline() {
        FailureBreakdown breakdown = FailureBreakdown
                .of(List.of(new FeatureScenarios("F", List.of(failedAt("a", "com.x.BoomError", "Widget", "go", 7)))));

        String html = FailureBreakdownRenderer.html(breakdown);

        // width and table-layout are inline on the <table>, not only in a <style> class, because Outlook (Word engine)
        // ignores <style>-block table widths. The colgroup pins the four columns as percentages so they scale.
        assertThat(html)
                .contains("<table class=\"breakdown\" width=\"100%\" " + "style=\"width:100%;table-layout:fixed\">");
        assertThat(html).contains("<col style=\"width:22%\"><col style=\"width:33%\"><col style=\"width:33%\">"
                + "<col style=\"width:12%\">");
    }

    @Test
    @DisplayName("an unmapped type is humanised: the Exception/Error suffix is dropped and CamelCase becomes words")
    void humanisesAnUnmappedType() {
        FailureBreakdown breakdown = FailureBreakdown.of(List.of(new FeatureScenarios("F",
                List.of(failedAt("a", "com.x.FreeSpinNotAwardedException", "RewardsBL", "award", 10)))));

        String html = FailureBreakdownRenderer.html(breakdown);

        assertThat(html).contains("<td>Free spin not awarded</td>");
        // The raw class name is still shown in the Where column so an engineer has the exact type.
        assertThat(html).contains("FreeSpinNotAwardedException");
    }

    @Test
    @DisplayName("the humaniser leaves a blank type and Unclassified untouched rather than inventing words")
    void humaniserLeavesBlankAndUnclassified() {
        assertThat(FailureBreakdownRenderer.HUMANIZED_TYPE.apply("")).isEmpty();
        assertThat(FailureBreakdownRenderer.HUMANIZED_TYPE.apply("Unclassified")).isEqualTo("Unclassified");
    }

    @Test
    @DisplayName("one location reached by several steps becomes one group with a nested row per step")
    void htmlNestsStepsUnderAGroup() {
        FailureBreakdown breakdown = FailureBreakdown
                .of(List.of(new FeatureScenarios("F", List.of(softFailure("a", "reduced by 1.0"),
                        softFailure("b", "reduced by 1.0"), softFailure("c", "increased by 5.0")))));

        String html = FailureBreakdownRenderer.html(breakdown);

        // One group header (subtotal 3) at the shared location, then a nested row per distinct step with its count.
        // The no-label overload humanises the type, and the Where cell stacks the location above the raw class.
        assertThat(html).contains("<tr><td>Assert j multiple failures</td>"
                + "<td>BalanceTracking.assertDelta:182<br>AssertJMultipleFailuresError</td>"
                + "<td></td><td class=\"count\">\u00d73</td></tr>");
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
