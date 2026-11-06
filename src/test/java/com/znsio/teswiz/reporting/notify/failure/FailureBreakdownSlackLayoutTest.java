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
 * The Slack layout of the failure breakdown: a heading that names the failure and cause counts, then one group per
 * cause (type and location) inside a code block. Each group is a header line — count and friendly label — then a second
 * line with the location and the raw class name, then one nested row per failing step with its own count. Splitting the
 * location and class onto their own line keeps each line short so a narrow Slack client does not wrap and misalign the
 * block. Only the top groups are listed; the rest are summarised in one line pointing to the full report.
 */
class FailureBreakdownSlackLayoutTest {

    private static final String FENCE = "```";

    // A consumer's friendly label: name PoolExhaustedException, let everything else fall back to its simple class name.
    private static final Function<String, String> FRIENDLY = type -> "PoolExhaustedException".equals(type)
            ? "No test user available"
            : type;

    @Test
    @DisplayName("slack shows a counted heading, then a two-line group header with the location and raw class")
    void twoLineGroupHeader() {
        FailureBreakdown breakdown = breakdownOf(
                failedAt("a", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                failedAt("b", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                failedAt("c", "com.x.PoolExhaustedException", "UserPool", "lease", 9));

        String slack = FailureBreakdownRenderer.slack(breakdown, FailureBreakdownRenderer.DEFAULT_MAX_CAUSES, FRIENDLY);

        assertThat(slack).startsWith("*Why did things fail?*  3 failures \u00b7 2 causes\n" + FENCE + "\n");
        assertThat(slack).contains("\u00d72  IndexOutOfBoundsException\n    RewardsBL.awardFreeSpins:113 "
                + "(IndexOutOfBoundsException)\n");
        assertThat(slack).contains("\u00d71  No test user available\n    UserPool.lease:9 (PoolExhaustedException)\n");
    }

    @Test
    @DisplayName("one location reached by several steps is one group with a nested row per step and its count")
    void nestedStepsPerGroup() {
        FailureBreakdown breakdown = breakdownOf(softFailure("a", "reduced by 1.0"), softFailure("b", "reduced by 1.0"),
                softFailure("c", "increased by 5.0"));

        String slack = FailureBreakdownRenderer.slack(breakdown);

        assertThat(slack).startsWith("*Why did things fail?*  3 failures \u00b7 1 cause\n");
        assertThat(slack).contains("\u00d73  AssertJMultipleFailuresError\n"
                + "    BalanceTracking.assertDelta:182 (AssertJMultipleFailuresError)\n"
                + "    \u2514 Then the balance is reduced by 1.0    \u00d72\n"
                + "    \u2514 Then the balance is increased by 5.0  \u00d71\n");
    }

    @Test
    @DisplayName("slack lists the top 10 groups by default and summarises the rest after the code block")
    void listsTheTopGroupsOnly() {
        String slack = FailureBreakdownRenderer.slack(twelveGroups());

        assertThat(slack).startsWith("*Why did things fail?*  12 failures \u00b7 12 causes\n");
        assertThat(slack).contains("Error10").doesNotContain("Error11").doesNotContain("Error12");
        assertThat(slack).endsWith(FENCE + "\n_+2 more causes in the full report_\n");
    }

    @Test
    @DisplayName("the number of groups listed in slack is configurable")
    void maxCausesIsConfigurable() {
        String slack = FailureBreakdownRenderer.slack(twelveGroups(), 1);

        assertThat(slack).contains("Error01").doesNotContain("Error02");
        assertThat(slack).endsWith(FENCE + "\n_+11 more causes in the full report_\n");
    }

    @Test
    @DisplayName("slack uses singular wording for one failure, one cause and one more cause")
    void singularWording() {
        FailureBreakdown one = breakdownOf(failedAt("a", "com.x.BoomError", "Widget", "go", 7));
        FailureBreakdown two = breakdownOf(failedAt("a", "com.x.BoomError", "Widget", "go", 7),
                failedAt("b", "com.x.BustError", "Widget", "go", 8));

        assertThat(FailureBreakdownRenderer.slack(one))
                .startsWith("*Why did things fail?*  1 failure \u00b7 1 cause\n");
        assertThat(FailureBreakdownRenderer.slack(two, 1)).endsWith("_+1 more cause in the full report_\n");
    }

    @Test
    @DisplayName("a backtick in a cause cannot break out of the slack code block")
    void backticksAreNeutralised() {
        FailureBreakdown breakdown = breakdownOf(failedAt("a", "com.x.Odd`Error", "Widget", "go", 7));

        String slack = FailureBreakdownRenderer.slack(breakdown);

        assertThat(slack).contains("\u00d71  Odd'Error\n");
        assertThat(slack.replace(FENCE, "")).doesNotContain("`");
    }

    @Test
    @DisplayName("a constructor-frame place keeps its <init> in slack (escaped so slack does not drop it)")
    void constructorFramePlaceSurvives() {
        FailureBreakdown breakdown = breakdownOf(failedAt("a", "com.x.BoomError", "Widget", "<init>", 7));

        String slack = FailureBreakdownRenderer.slack(breakdown);

        assertThat(slack).contains("Widget.&lt;init&gt;:7").doesNotContain("<init>");
    }

    @Test
    @DisplayName("slack escapes & and > in a type so slack does not read them as markup")
    void ampersandAndAngleInTypeAreEscaped() {
        FailureBreakdown breakdown = breakdownOf(failedAt("a", "com.x.A&B>CError", "Widget", "go", 7));

        String slack = FailureBreakdownRenderer.slack(breakdown);

        assertThat(slack).contains("\u00d71  A&amp;B&gt;CError\n");
    }

    @Test
    @DisplayName("slack escapes markup in the +K more causes line too")
    void moreCausesLineIsEscaped() {
        FailureBreakdown breakdown = breakdownOf(failedAt("a", "com.x.BoomError", "Widget", "go", 7),
                failedAt("b", "com.x.A<B>Error", "Widget", "go", 8));

        String slack = FailureBreakdownRenderer.slack(breakdown, 1);

        assertThat(slack).doesNotContain("<").doesNotContain(">");
    }

    private static FailureBreakdown twelveGroups() {
        Scenario[] scenarios = new Scenario[12];
        for (int i = 1; i <= 12; i++) {
            scenarios[i - 1] = failedAt("s" + i, String.format("com.x.Error%02d", i), "Widget", "go", i);
        }
        return breakdownOf(scenarios);
    }

    private static FailureBreakdown breakdownOf(Scenario... scenarios) {
        return FailureBreakdown.of(List.of(new FeatureScenarios("F", List.of(scenarios))));
    }

    private static Scenario failedAt(String name, String type, String className, String method, int line) {
        String trace = type + ": x\n\tat com.app." + className + "." + method + "(" + className + ".java:" + line
                + ")\n";
        return new Scenario(name, ScenarioStatus.FAILED, "x", 0.1, type, trace);
    }

    // A soft-assertion failure at BalanceTracking.assertDelta:182 on the given step, shaped like the real JUnit
    // message so the classifier reads both the location and the step from it.
    private static Scenario softFailure(String name, String what) {
        String message = "\nMultiple Failures (1 failure)\n-- failure 1 --\n[step 4: Then the balance is " + what
                + "] [desc]\nat BalanceTracking.assertDelta(BalanceTracking.java:182)";
        return new Scenario(name, ScenarioStatus.FAILED, message, 0.1,
                "org.assertj.core.error.AssertJMultipleFailuresError", message);
    }
}
