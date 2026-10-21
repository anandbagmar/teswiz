package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class XaiLlmVisionProviderTest {

    private final XaiLlmVisionProvider provider = new XaiLlmVisionProvider();

    @Test
    void buildHeaders_usesBearerAuthorization() {
        assertThat(provider.buildHeaders("xai-secret")).containsEntry("Authorization", "Bearer xai-secret");
    }

    @Test
    void healthCheckProbe_derivesXaiModelsListingFromChatCompletions() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "https://api.x.ai/v1/chat/completions", "xai-secret").orElseThrow();

        assertThat(probe.url()).isEqualTo("https://api.x.ai/v1/models");
        assertThat(probe.headers()).containsEntry("Authorization", "Bearer xai-secret");
    }
}
