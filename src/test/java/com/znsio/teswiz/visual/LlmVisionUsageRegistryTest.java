package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LlmVisionUsageRegistryTest {

    @BeforeEach
    @AfterEach
    void clearRegistry() {
        LlmVisionUsageRegistry.reset();
    }

    @Test
    void getRunTotalsAggregatesAcrossCommittedTests() {
        LlmVisionUsage t1 = new LlmVisionUsage();
        t1.record(100, 10, 110, 400);
        t1.recordCapture("Login", "p/01-login.png", "login.png");

        LlmVisionUsage t2 = new LlmVisionUsage();
        t2.record(50, 5, 55, 600);
        t2.record(20, 2, 22, 100);

        LlmVisionUsageRegistry.commit("scenario-a_1", t1);
        LlmVisionUsageRegistry.commit("scenario-b_1", t2);

        LlmVisionUsageRegistry.RunTotals totals = LlmVisionUsageRegistry.getRunTotals();
        assertThat(totals.testsUsingLlm()).isEqualTo(2);
        assertThat(totals.totalCalls()).isEqualTo(3);
        assertThat(totals.promptTokens()).isEqualTo(170);
        assertThat(totals.completionTokens()).isEqualTo(17);
        assertThat(totals.totalTokens()).isEqualTo(187);
        assertThat(totals.totalDurationMs()).isEqualTo(1100);
        assertThat(totals.totalDurationSeconds()).isEqualTo(1.1);
        assertThat(totals.totalCaptures()).isEqualTo(1);
    }

    @Test
    void commitIgnoresNullKeyNullUsageAndInactiveUsage() {
        LlmVisionUsageRegistry.commit(null, new LlmVisionUsage());
        LlmVisionUsageRegistry.commit("k", null);
        LlmVisionUsageRegistry.commit("inactive", new LlmVisionUsage()); // no activity

        assertThat(LlmVisionUsageRegistry.hasAnyActivity()).isFalse();
        assertThat(LlmVisionUsageRegistry.getRunTotals().testsUsingLlm()).isZero();
    }

    @Test
    void aCaptureOnlyUsageCountsAsActivity() {
        LlmVisionUsage captureOnly = new LlmVisionUsage();
        captureOnly.recordCapture("Icon", "p/01-icon.png", "icon.png");

        LlmVisionUsageRegistry.commit("capture-only_1", captureOnly);

        assertThat(LlmVisionUsageRegistry.hasAnyActivity()).isTrue();
        assertThat(LlmVisionUsageRegistry.getRunTotals().totalCaptures()).isEqualTo(1);
        assertThat(LlmVisionUsageRegistry.getRunTotals().totalCalls()).isZero();
    }

    @Test
    void resetClearsAllAccumulatedState() {
        LlmVisionUsage usage = new LlmVisionUsage();
        usage.record(10, 1, 11, 100);
        LlmVisionUsageRegistry.commit("s_1", usage);
        assertThat(LlmVisionUsageRegistry.hasAnyActivity()).isTrue();

        LlmVisionUsageRegistry.reset();

        assertThat(LlmVisionUsageRegistry.hasAnyActivity()).isFalse();
        assertThat(LlmVisionUsageRegistry.getRunTotals().testsUsingLlm()).isZero();
    }

    @Test
    void logRunSummaryIsSilentAndSafeWhenNoActivity() {
        // no commits -> must not throw
        LlmVisionUsageRegistry.logRunSummary();
        assertThat(LlmVisionUsageRegistry.hasAnyActivity()).isFalse();
    }
}
