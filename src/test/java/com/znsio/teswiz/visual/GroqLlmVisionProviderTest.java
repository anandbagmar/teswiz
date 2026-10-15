package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class GroqLlmVisionProviderTest {

    private static final byte[] SCREENSHOT = {1, 2, 3, 4};

    private final GroqLlmVisionProvider provider = new GroqLlmVisionProvider();

    @Test
    void buildRequestBody_matchesOpenAiWireFormat() {
        JSONObject body = new JSONObject(
                provider.buildRequestBody("system-prompt", "find it", SCREENSHOT, List.of(), "llama-vision"));

        assertThat(body.getString("model")).isEqualTo("llama-vision");
        assertThat(body.getJSONArray("messages").getJSONObject(0).getString("role")).isEqualTo("system");
    }

    @Test
    void buildHeaders_usesBearerAuthorization() {
        assertThat(provider.buildHeaders("gsk-secret")).containsEntry("Authorization", "Bearer gsk-secret");
    }

    @Test
    void healthCheckProbe_derivesGroqModelsListingFromChatCompletions() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "https://api.groq.com/openai/v1/chat/completions", "gsk-secret").orElseThrow();

        assertThat(probe.url()).isEqualTo("https://api.groq.com/openai/v1/models");
        assertThat(probe.headers()).containsEntry("Authorization", "Bearer gsk-secret");
    }
}
