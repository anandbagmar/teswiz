package com.znsio.teswiz.filters.apitraffic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApiCallContext} — a per-scenario, per-thread call counter
 * that resets to start each scenario's API calls at index 01.
 */
class ApiCallContextTest {

    @AfterEach
    void tearDown() {
        ApiCallContext.clear();
    }

    @Test
    void nextIndex_startsAtOneAfterReset() {
        ApiCallContext.resetForNewScenario();
        assertThat(ApiCallContext.nextIndex()).isEqualTo(1);
    }

    @Test
    void nextIndex_incrementsWithinSameScenario() {
        ApiCallContext.resetForNewScenario();
        assertThat(ApiCallContext.nextIndex()).isEqualTo(1);
        assertThat(ApiCallContext.nextIndex()).isEqualTo(2);
        assertThat(ApiCallContext.nextIndex()).isEqualTo(3);
    }

    @Test
    void resetForNewScenario_restartsCounterAtOne() {
        ApiCallContext.resetForNewScenario();
        ApiCallContext.nextIndex();
        ApiCallContext.nextIndex();

        ApiCallContext.resetForNewScenario();
        assertThat(ApiCallContext.nextIndex()).isEqualTo(1);
    }

    @Test
    void nextIndex_withoutReset_stillStartsAtOne() {
        assertThat(ApiCallContext.nextIndex()).isEqualTo(1);
    }
}
