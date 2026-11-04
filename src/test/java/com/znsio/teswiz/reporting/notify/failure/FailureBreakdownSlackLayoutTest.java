package com.znsio.teswiz.reporting.notify.failure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.reporting.notify.JUnitResults.FeatureScenarios;
import com.znsio.teswiz.reporting.notify.JUnitResults.Scenario;
import com.znsio.teswiz.reporting.notify.JUnitResults.ScenarioStatus;

/**
 * The Slack layout of the failure breakdown: a heading that names the failure and cause counts, then one aligned row
 * per cause inside a code block (count, type, place), so the block reads as a table in Slack. Only the top causes are
 * listed; the rest are summarised in one line that points to the full report.
 */
class FailureBreakdownSlackLayoutTest {

    private static final String FENCE = "```";

    @Test
    @DisplayName("slack shows a counted heading, then aligned count-first rows in a code block")
    void alignedCountFirstBlock() {
        FailureBreakdown breakdown = breakdownOf(
                failedAt("a", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                failedAt("b", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                failedAt("c", "com.x.PoolExhaustedException", "UserPool", "lease", 9),
                new Scenario("d", ScenarioStatus.FAILED, "no type", 0.1));

        assertThat(FailureBreakdownRenderer.slack(breakdown)).isEqualTo(
                "*Why did things fail?*  4 failures · 3 causes\n"
                        + FENCE + "\n"
                        + "2  IndexOutOfBoundsException  RewardsBL.awardFreeSpins:113\n"
                        + "1  PoolExhaustedException     UserPool.lease:9\n"
                        + "1  Unclassified\n"
                        + FENCE + "\n");
    }

    @Test
    @DisplayName("slack right-aligns counts of different widths")
    void rightAlignsCounts() {
        List<Scenario> scenarios = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            scenarios.add(failedAt("big" + i, "com.x.BigError", "Big", "fail", 1));
        }
        scenarios.add(failedAt("small", "com.x.SmallError", "Small", "fail", 2));

        String slack = FailureBreakdownRenderer.slack(breakdownOf(scenarios.toArray(new Scenario[0])));

        assertThat(slack).contains("12  BigError    Big.fail:1\n");
        assertThat(slack).contains(" 1  SmallError  Small.fail:2\n");
    }

    @Test
    @DisplayName("slack lists the top 10 causes by default and summarises the rest after the code block")
    void listsTheTopCausesOnly() {
        String slack = FailureBreakdownRenderer.slack(twelveCauses());

        assertThat(slack).startsWith("*Why did things fail?*  12 failures · 12 causes\n");
        assertThat(slack).contains("Error10").doesNotContain("Error11").doesNotContain("Error12");
        assertThat(slack).endsWith(FENCE + "\n_+2 more causes in the full report_\n");
    }

    @Test
    @DisplayName("the number of causes listed in slack is configurable")
    void maxCausesIsConfigurable() {
        String slack = FailureBreakdownRenderer.slack(twelveCauses(), 1);

        assertThat(slack).contains("Error01").doesNotContain("Error02");
        assertThat(slack).endsWith(FENCE + "\n_+11 more causes in the full report_\n");
    }

    @Test
    @DisplayName("slack uses singular wording for one failure, one cause and one more cause")
    void singularWording() {
        FailureBreakdown one = breakdownOf(failedAt("a", "com.x.BoomError", "Widget", "go", 7));
        FailureBreakdown two = breakdownOf(failedAt("a", "com.x.BoomError", "Widget", "go", 7),
                failedAt("b", "com.x.BustError", "Widget", "go", 8));

        assertThat(FailureBreakdownRenderer.slack(one)).startsWith("*Why did things fail?*  1 failure · 1 cause\n");
        assertThat(FailureBreakdownRenderer.slack(two, 1)).endsWith("_+1 more cause in the full report_\n");
    }

    @Test
    @DisplayName("a backtick in a cause cannot break out of the slack code block")
    void backticksAreNeutralised() {
        FailureBreakdown breakdown = breakdownOf(failedAt("a", "com.x.Odd`Error", "Widget", "go", 7));

        String slack = FailureBreakdownRenderer.slack(breakdown);

        assertThat(slack).contains("1  Odd'Error  Widget.go:7\n");
        assertThat(slack.replace(FENCE, "")).doesNotContain("`");
    }

    @Test
    @DisplayName("slack pads the type column to the causes shown, so a long hidden type does not widen the rows")
    void padsToTheShownCausesOnly() {
        FailureBreakdown breakdown = breakdownOf(failedAt("a", "com.x.ShortError", "Widget", "go", 1),
                failedAt("b", "com.x.ShortError", "Widget", "go", 1),
                failedAt("c", "com.x.AVeryMuchLongerHiddenException", "Widget", "go", 2));

        String slack = FailureBreakdownRenderer.slack(breakdown, 1);

        assertThat(slack).contains(FENCE + "\n2  ShortError  Widget.go:1\n" + FENCE + "\n");
    }

    private static FailureBreakdown twelveCauses() {
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
}
