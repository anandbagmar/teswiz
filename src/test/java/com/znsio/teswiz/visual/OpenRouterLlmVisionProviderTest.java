package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenRouterLlmVisionProviderTest {

    private final OpenRouterLlmVisionProvider provider = new OpenRouterLlmVisionProvider();

    @Test
    void buildHeaders_usesBearerAuthorization() {
        assertThat(provider.buildHeaders("or-secret")).containsEntry("Authorization", "Bearer or-secret");
    }

    @Test
    void healthCheckProbe_derivesOpenRouterModelsListingFromChatCompletions() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "https://openrouter.ai/api/v1/chat/completions", "or-secret").orElseThrow();

        assertThat(probe.url()).isEqualTo("https://openrouter.ai/api/v1/models");
        assertThat(probe.headers()).containsEntry("Authorization", "Bearer or-secret");
    }
}
