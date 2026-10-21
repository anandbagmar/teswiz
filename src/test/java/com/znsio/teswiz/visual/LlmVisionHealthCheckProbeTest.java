package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LlmVisionHealthCheckProbeTest {

    @Test
    void storesUrlAndDefensivelyCopiesHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");

        LlmVisionHealthCheckProbe probe =
                new LlmVisionHealthCheckProbe("https://api.example.com/v1/models", headers);

        assertThat(probe.url()).isEqualTo("https://api.example.com/v1/models");
        assertThat(probe.headers()).containsEntry("Authorization", "Bearer token");

        // mutating the source map must not affect the stored headers (defensive copy)
        headers.put("X-Extra", "value");
        assertThat(probe.headers()).doesNotContainKey("X-Extra");
    }

    @Test
    void nullHeadersBecomeAnEmptyMap() {
        LlmVisionHealthCheckProbe probe =
                new LlmVisionHealthCheckProbe("https://api.example.com/v1/models", null);

        assertThat(probe.headers()).isEmpty();
    }

    @Test
    void storedHeadersAreUnmodifiable() {
        LlmVisionHealthCheckProbe probe =
                new LlmVisionHealthCheckProbe("https://api.example.com/v1/models", Map.of("k", "v"));

        assertThatThrownBy(() -> probe.headers().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void blankOrNullUrlIsRejected() {
        assertThatThrownBy(() -> new LlmVisionHealthCheckProbe("", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
        assertThatThrownBy(() -> new LlmVisionHealthCheckProbe(null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
