package com.znsio.teswiz.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.network.NetworkNote;

/**
 * Unit tests for {@link ScenarioNetworkNotes}: a network note recorded while a scenario runs is kept with that
 * scenario (so a consumer's {@code @After} hook can attach it to the Cucumber report) and sent to ReportPortal. A note
 * recorded outside a scenario (for example by the end-of-run notification) belongs to no scenario.
 */
class ScenarioNetworkNotesTest {

    private static final NetworkNote NOTE = new NetworkNote("api", "wallet.example", 443,
            List.of(new NetworkNote.UnreachableAddress("10.0.0.1", "ConnectException: Connection refused")),
            "10.0.0.2");

    @AfterEach
    void removeTheScenarioContext() {
        SessionContext.remove(Thread.currentThread().getId());
    }

    @Test
    void aNoteRecordedDuringAScenario_isKeptWithThatScenario() {
        TestExecutionContext scenario = new TestExecutionContext("a scenario");

        ScenarioNetworkNotes.record(NOTE);
        ScenarioNetworkNotes.record(NOTE);

        assertThat(ScenarioNetworkNotes.of(scenario)).containsExactly(NOTE.describe(), NOTE.describe());
        assertThat(scenario.getTestState(TEST_CONTEXT.NETWORK_NOTES)).isNotNull();
    }

    @Test
    void aScenarioWithoutNotes_hasNone() {
        TestExecutionContext scenario = new TestExecutionContext("a quiet scenario");

        assertThat(ScenarioNetworkNotes.of(scenario)).isEmpty();
    }

    @Test
    void aNoteRecordedOutsideAnyScenario_isIgnoredWithoutFailing() {
        SessionContext.remove(Thread.currentThread().getId());

        ScenarioNetworkNotes.record(NOTE);

        assertThat(SessionContext.getTestExecutionContext(Thread.currentThread().getId())).isNull();
    }
}
