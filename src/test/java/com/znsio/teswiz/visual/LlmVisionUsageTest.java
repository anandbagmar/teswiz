package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmVisionUsageTest {

    @Test
    void recordAccumulatesTokensAndDuration() {
        LlmVisionUsage usage = new LlmVisionUsage();
        usage.record(100, 10, 110, 500);
        usage.record(50, 5, 55, 250);

        assertThat(usage.getCalls()).isEqualTo(2);
        assertThat(usage.getPromptTokens()).isEqualTo(150);
        assertThat(usage.getCompletionTokens()).isEqualTo(15);
        assertThat(usage.getTotalTokens()).isEqualTo(165);
        assertThat(usage.getTotalDurationSeconds()).isEqualTo(0.75);
    }

    @Test
    void recordCaptureTracksCountAndRecordsInOrder() {
        LlmVisionUsage usage = new LlmVisionUsage();
        usage.recordCapture("Login Button", "reports/s/llm-vision-captures/01-login-button.png", "login-button.png");
        usage.recordCapture("Cart Icon", "reports/s/llm-vision-captures/02-cart-icon.png", "cart-icon.png");

        assertThat(usage.getCaptureCount()).isEqualTo(2);
        assertThat(usage.getCaptures()).hasSize(2);
        assertThat(usage.getCaptures().get(0).description()).isEqualTo("Login Button");
        assertThat(usage.getCaptures().get(0).suggestedFileName()).isEqualTo("login-button.png");
        assertThat(usage.getCaptures().get(1).savedPath()).endsWith("02-cart-icon.png");
    }

    @Test
    void snapshotCopiesCapturesAndCounters() {
        LlmVisionUsage usage = new LlmVisionUsage();
        usage.record(10, 1, 11, 100);
        usage.recordCapture("A", "p/01-a.png", "a.png");

        LlmVisionUsage snap = usage.snapshot();
        // mutating the original must not affect the snapshot
        usage.recordCapture("B", "p/02-b.png", "b.png");

        assertThat(snap.getCaptureCount()).isEqualTo(1);
        assertThat(snap.getCaptures()).hasSize(1);
        assertThat(snap.getTotalTokens()).isEqualTo(11);
    }

    @Test
    void capturesListIsUnmodifiable() {
        LlmVisionUsage usage = new LlmVisionUsage();
        usage.recordCapture("A", "p/01-a.png", "a.png");
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> usage.getCaptures().clear());
    }
}
