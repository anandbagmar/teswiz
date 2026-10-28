package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.znsio.teswiz.reporting.notify.JUnitResults.FeatureScenarios;
import com.znsio.teswiz.reporting.notify.JUnitResults.Scenario;
import com.znsio.teswiz.reporting.notify.JUnitResults.ScenarioStatus;

/**
 * Unit tests for {@link JUnitResults}.
 * <p>
 * The parse reads a cucumber/JUnit {@code <testsuite>}: the suite counts and total run time, every scenario grouped by
 * its {@code classname} (the Gherkin feature name) with each scenario's status, failure/skip reason and run time, and
 * the failures-only view derived from that same scenario list. These tests pin the mixed fixture (5 scenarios across 3
 * features: 2 passed, 2 failed, 1 skipped) so a regression in grouping, status detection, reason extraction or timing
 * is caught offline.
 */
class JUnitResultsTest {

    private static final String MIXED_FIXTURE = "/notify/cucumber-junit-mixed.xml";

    @Test
    void parsesSuiteCountsAndDuration() {
        JUnitResults results = parse(MIXED_FIXTURE);

        assertThat(results.totalTests()).isEqualTo(5);
        assertThat(results.failed()).isEqualTo(2);
        assertThat(results.errors()).isEqualTo(0);
        assertThat(results.skipped()).isEqualTo(1);
        assertThat(results.passed()).isEqualTo(2);
        assertThat(results.hasFailures()).isTrue();
        assertThat(results.isEmpty()).isFalse();
        assertThat(results.durationSeconds()).isEqualTo(42.5);
    }

    @Test
    void groupsEveryScenarioByFeatureInDocumentOrder() {
        JUnitResults results = parse(MIXED_FIXTURE);

        List<FeatureScenarios> byFeature = results.scenariosByFeature();

        assertThat(byFeature).extracting(FeatureScenarios::feature).containsExactly("In-Game Bet", "Void", "Balance");
        assertThat(byFeature.get(0).scenarios()).hasSize(2);
        assertThat(byFeature.get(1).scenarios()).hasSize(2);
        assertThat(byFeature.get(2).scenarios()).hasSize(1);
    }

    @Test
    void capturesStatusReasonAndTimePerScenario() {
        JUnitResults results = parse(MIXED_FIXTURE);
        List<Scenario> bets = results.scenariosByFeature().get(0).scenarios();

        Scenario passed = bets.get(0);
        assertThat(passed.status()).isEqualTo(ScenarioStatus.PASSED);
        assertThat(passed.reason()).isEmpty();
        assertThat(passed.time()).isEqualTo(6.125);

        Scenario failed = bets.get(1);
        assertThat(failed.status()).isEqualTo(ScenarioStatus.FAILED);
        assertThat(failed.reason()).isEqualTo("Expected bet to be accepted but it was rejected with INSUFFICIENT_FUNDS");
        assertThat(failed.time()).isEqualTo(1.5);
    }

    @Test
    void marksBareSkippedScenarioWithoutFabricatingAReason() {
        JUnitResults results = parse(MIXED_FIXTURE);

        Scenario skipped = results.scenariosByFeature().get(2).scenarios().get(0);

        assertThat(skipped.status()).isEqualTo(ScenarioStatus.SKIPPED);
        assertThat(skipped.skipped()).isTrue();
        assertThat(skipped.reason()).isEmpty();
    }

    @Test
    void failuresViewIsTheScenarioViewFilteredToFailures() {
        JUnitResults results = parse(MIXED_FIXTURE);

        List<JUnitResults.FeatureFailures> failures = results.failuresByFeature();

        assertThat(failures).extracting(JUnitResults.FeatureFailures::feature).containsExactly("In-Game Bet", "Void");
        assertThat(failures.get(0).scenarios()).containsExactly("Place a bet above the balance is rejected");
        assertThat(failures.get(1).scenarios()).containsExactly("Void a non-existent bet is rejected");
    }

    private JUnitResults parse(String resource) {
        return JUnitResults.fromXml(resourcePath(resource));
    }

    private Path resourcePath(String resource) {
        URL url = getClass().getResource(resource);
        assertThat(url).as("fixture %s is on the test classpath", resource).isNotNull();
        try {
            return Paths.get(url.toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve fixture " + resource, e);
        }
    }
}
