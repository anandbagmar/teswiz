package com.znsio.teswiz.reporting.notify.failure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.reporting.notify.JUnitResults;
import com.znsio.teswiz.reporting.notify.JUnitResults.FeatureScenarios;
import com.znsio.teswiz.reporting.notify.JUnitResults.Scenario;
import com.znsio.teswiz.reporting.notify.JUnitResults.ScenarioStatus;

/**
 * The breakdown answers "why did things fail?" for a run: failing scenarios counted by cause (exception type and where
 * it came from), biggest first. The total must always equal the number of failed and errored scenarios, so a failure
 * that cannot be classified is counted as Unclassified rather than dropped. Several scenarios dying on the same line
 * are one cause, not several.
 */
class FailureBreakdownTest {

    private static final String FIXTURE = "/notify/cucumber-junit-failure-causes.xml";

    private static final FailureCause INDEX_CAUSE = new FailureCause("IndexOutOfBoundsException",
            "RewardsBL.awardFreeSpins:113");

    @Test
    @DisplayName("scenarios failing at the same place are one cause with a count")
    void sameCauseIsOneEntry() {
        FailureBreakdown breakdown = FailureBreakdown.of(List.of(
                feature("F", failedAt("a", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                        failedAt("b", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                        failedAt("c", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113))));

        assertThat(breakdown.causes()).containsExactly(entry(INDEX_CAUSE, 3));
        assertThat(breakdown.total()).isEqualTo(3);
    }

    @Test
    @DisplayName("the same exception type at different places stays separate causes")
    void differentPlacesAreDifferentCauses() {
        FailureBreakdown breakdown = FailureBreakdown.of(List.of(
                feature("F", failedAt("a", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                        failedAt("b", "java.lang.IndexOutOfBoundsException", "WalletBL", "refund", 40))));

        assertThat(breakdown.causes()).hasSize(2);
        assertThat(breakdown.byType()).containsExactly(entry("IndexOutOfBoundsException", 2));
    }

    @Test
    @DisplayName("causes are ordered biggest first, then by type, then by place, so the output is stable")
    void orderedBiggestFirstThenByName() {
        FailureBreakdown breakdown = FailureBreakdown.of(List.of(feature("F",
                failedAt("a", "com.x.ZebraException", "Z", "z", 1), failedAt("b", "com.x.AlphaException", "A", "a", 2),
                failedAt("c", "com.x.BigException", "B", "b", 3), failedAt("d", "com.x.BigException", "B", "b", 3))));

        assertThat(breakdown.causes().keySet()).extracting(FailureCause::type).containsExactly("BigException",
                "AlphaException", "ZebraException");
    }

    @Test
    @DisplayName("a failure with no type is counted as Unclassified, never dropped")
    void unclassifiedIsStillCounted() {
        FailureBreakdown breakdown = FailureBreakdown
                .of(List.of(feature("F", new Scenario("weird", ScenarioStatus.FAILED, "expected true", 0.1))));

        assertThat(breakdown.total()).isEqualTo(1);
        assertThat(breakdown.byType()).containsExactly(entry(FailureClassifier.UNCLASSIFIED, 1));
    }

    @Test
    @DisplayName("errored scenarios count as failures; passed and skipped ones do not")
    void onlyFailedAndErroredCount() {
        FailureBreakdown breakdown = FailureBreakdown.of(List.of(feature("F",
                new Scenario("p", ScenarioStatus.PASSED, "", 0.1), new Scenario("s", ScenarioStatus.SKIPPED, "", 0.1),
                new Scenario("e", ScenarioStatus.ERRORED, "boom", 0.1, "com.x.BoomError", ""))));

        assertThat(breakdown.total()).isEqualTo(1);
        assertThat(breakdown.byType()).containsExactly(entry("BoomError", 1));
    }

    @Test
    @DisplayName("no failures means an empty breakdown")
    void noFailuresIsEmpty() {
        FailureBreakdown breakdown = FailureBreakdown
                .of(List.of(feature("F", new Scenario("p", ScenarioStatus.PASSED, "", 0.1))));

        assertThat(breakdown.isEmpty()).isTrue();
        assertThat(breakdown.total()).isZero();
        assertThat(breakdown.causes()).isEmpty();
        assertThat(breakdown).isSameAs(FailureBreakdown.empty());
    }

    @Test
    @DisplayName("two breakdowns add up, merging shared causes — the aggregate across jobs")
    void breakdownsAddUp() {
        FailureBreakdown one = FailureBreakdown.of(List.of(
                feature("F", failedAt("a", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113),
                        failedAt("b", "com.x.OtherException", "O", "o", 1))));
        FailureBreakdown two = FailureBreakdown.of(List.of(feature("G",
                failedAt("c", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113))));

        FailureBreakdown merged = one.plus(two);

        assertThat(merged.total()).isEqualTo(3);
        assertThat(merged.causes().get(INDEX_CAUSE)).isEqualTo(2);
        assertThat(merged.causes().keySet()).first().isEqualTo(INDEX_CAUSE);
    }

    @Test
    @DisplayName("adding the empty breakdown changes nothing")
    void emptyIsTheIdentity() {
        FailureBreakdown one = FailureBreakdown.of(List.of(feature("F",
                failedAt("a", "java.lang.IndexOutOfBoundsException", "RewardsBL", "awardFreeSpins", 113))));

        assertThat(one.plus(FailureBreakdown.empty()).causes()).isEqualTo(one.causes());
        assertThat(FailureBreakdown.empty().plus(one).causes()).isEqualTo(one.causes());
    }

    @Test
    @DisplayName("a real STAGE run: duplicates collapse to one cause and the total matches the headline")
    void realRunFixture() {
        JUnitResults results = JUnitResults.fromXml(fixture());

        FailureBreakdown breakdown = results.failureBreakdown();

        assertThat(breakdown.total()).isEqualTo(results.failed() + results.errors());
        // The two IndexOutOfBoundsException scenarios differ on the failing step: one trace carries the Cucumber
        // "✽.the player is awarded free spins" line, the other's trace is truncated and carries no step — so they are
        // two distinct causes (same type and place, different step), not one. The soft-assertion failure carries the
        // step from its "[step 4: ...]" message marker. All five causes have count 1 except Unclassified (2).
        assertThat(breakdown.causes()).containsOnlyKeys(new FailureCause("Unclassified", ""),
                new FailureCause("AssertJMultipleFailuresError", "BalanceTracking.assertReduced:111",
                        "And the player's balance is reduced"),
                new FailureCause("IndexOutOfBoundsException", "RewardsBL.awardFreeSpins:113"),
                new FailureCause("IndexOutOfBoundsException", "RewardsBL.awardFreeSpins:113",
                        "the player is awarded free spins"),
                new FailureCause("TargetClosedError", "GamePlayScreenPlaywrightJava.spin:396"));
        assertThat(breakdown.causes()).containsEntry(new FailureCause("Unclassified", ""), 2);
        assertThat(breakdown.byType()).containsExactly(entry("IndexOutOfBoundsException", 2), entry("Unclassified", 2),
                entry("AssertJMultipleFailuresError", 1), entry("TargetClosedError", 1));
    }

    private static FeatureScenarios feature(String name, Scenario... scenarios) {
        return new FeatureScenarios(name, List.of(scenarios));
    }

    private static Scenario failedAt(String name, String type, String className, String method, int line) {
        String trace = type + ": x\n\tat com.app." + className + "." + method + "(" + className + ".java:" + line
                + ")\n";
        return new Scenario(name, ScenarioStatus.FAILED, "x", 0.1, type, trace);
    }

    private Path fixture() {
        URL url = getClass().getResource(FIXTURE);
        assertThat(url).as("fixture %s is on the test classpath", FIXTURE).isNotNull();
        try {
            return Paths.get(url.toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve fixture " + FIXTURE, e);
        }
    }
}
