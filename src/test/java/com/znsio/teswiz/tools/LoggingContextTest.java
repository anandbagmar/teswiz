package com.znsio.teswiz.tools;

import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingContextTest {
    @AfterEach
    void clearContext() {
        LoggingContext.clear();
    }

    @Test
    void shouldSetScenarioFieldsAndClearThem() {
        LoggingContext.begin("Checkout", 3, 2, "/tmp/reports/3-Checkout_2");

        assertThat(ThreadContext.get("scenario")).isEqualTo("Checkout");
        assertThat(ThreadContext.get("scenarioNumber")).isEqualTo("3");
        assertThat(ThreadContext.get("exampleRow")).isEqualTo("2");
        assertThat(ThreadContext.get("scenarioLogDirectory")).isEqualTo("/tmp/reports/3-Checkout_2");

        LoggingContext.clear();

        assertThat(ThreadContext.get("scenario")).isNull();
        assertThat(ThreadContext.get("scenarioNumber")).isNull();
        assertThat(ThreadContext.get("exampleRow")).isNull();
        assertThat(ThreadContext.get("scenarioLogDirectory")).isNull();
    }
}
