package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.znsio.teswiz.reporting.notify.JUnitResults.Scenario;
import com.znsio.teswiz.reporting.notify.JUnitResults.ScenarioStatus;

/**
 * A failed scenario carries more than its message: the exception class from the {@code <failure>/<error>} {@code type}
 * attribute and the stack trace from its body. Without them a reader only gets the human message — for example
 * {@code Index 0 out of bounds for length 0} — which never names the exception, so nothing downstream can say what
 * kind of failure it was or where it came from. These cases pin both new fields against a fixture built from a real
 * STAGE run.
 */
class JUnitResultsFailureDetailsTest {

    private static final String FIXTURE = "/notify/cucumber-junit-failure-causes.xml";

    @Test
    void capturesTheExceptionClassFromTheFailureTypeAttribute() {
        Scenario scenario = scenarioNamed("Award and play free spins");

        assertThat(scenario.status()).isEqualTo(ScenarioStatus.FAILED);
        assertThat(scenario.type()).isEqualTo("java.lang.IndexOutOfBoundsException");
        assertThat(scenario.reason()).isEqualTo("Index 0 out of bounds for length 0");
    }

    @Test
    void capturesTheExceptionClassFromAnErrorElementToo() {
        Scenario scenario = scenarioNamed("Browser closes mid-run");

        assertThat(scenario.status()).isEqualTo(ScenarioStatus.ERRORED);
        assertThat(scenario.type()).isEqualTo("com.microsoft.playwright.impl.TargetClosedError");
    }

    @Test
    void capturesTheStackTraceFromTheFailureBody() {
        Scenario scenario = scenarioNamed("Award and play free spins");

        assertThat(scenario.trace()).contains("at com.entain.casino.businessLayer.domain.RewardsBL.awardFreeSpins"
                + "(RewardsBL.java:113)");
    }

    @Test
    void aFailureWithNoTypeAttributeHasAnEmptyTypeRatherThanNull() {
        Scenario scenario = scenarioNamed("Failure with no type attribute");

        assertThat(scenario.type()).isEmpty();
        assertThat(scenario.reason()).isEqualTo("Something failed but the runner wrote no type");
    }

    @Test
    void aBlankTypeAttributeIsTrimmedToEmpty() {
        assertThat(scenarioNamed("Failure with a blank type attribute").type()).isEmpty();
    }

    @Test
    void aFailureWithNoBodyHasAnEmptyTrace() {
        assertThat(scenarioNamed("Failure with a blank type attribute").trace()).isEmpty();
    }

    @Test
    void aPassedScenarioHasNoTypeAndNoTrace() {
        Scenario scenario = scenarioNamed("Passing scenario");

        assertThat(scenario.type()).isEmpty();
        assertThat(scenario.trace()).isEmpty();
    }

    @Test
    void theFourArgumentConstructorStillWorksAndLeavesTypeAndTraceEmpty() {
        Scenario scenario = new Scenario("name", ScenarioStatus.FAILED, "reason", 1.5);

        assertThat(scenario.type()).isEmpty();
        assertThat(scenario.trace()).isEmpty();
        assertThat(scenario.failed()).isTrue();
    }

    private Scenario scenarioNamed(String name) {
        List<Scenario> all = JUnitResults.fromXml(fixture()).scenariosByFeature().stream()
                .flatMap(feature -> feature.scenarios().stream()).toList();
        return all.stream().filter(scenario -> scenario.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No scenario named '" + name + "' in " + FIXTURE));
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
