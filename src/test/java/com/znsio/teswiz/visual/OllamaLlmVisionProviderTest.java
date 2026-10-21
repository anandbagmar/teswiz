package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class OllamaLlmVisionProviderTest {

    private static final byte[] SCREENSHOT = {1, 2, 3, 4};

    private final OllamaLlmVisionProvider provider = new OllamaLlmVisionProvider();

    @Test
    void buildHeaders_sendsNoAuthorizationForLocalServer() {
        assertThat(provider.buildHeaders("ignored")).doesNotContainKey("Authorization");
        assertThat(provider.buildHeaders("ignored").get("content-type")).containsIgnoringCase("application/json");
    }

    @Test
    void buildRequestBody_matchesOpenAiWireFormat() {
        JSONObject body = new JSONObject(
                provider.buildRequestBody("system-prompt", "find it", SCREENSHOT, List.of(), "llava"));

        assertThat(body.getString("model")).isEqualTo("llava");
        assertThat(body.getJSONArray("messages").getJSONObject(0).getString("role")).isEqualTo("system");
    }

    @Test
    void healthCheckProbe_listsModelsWithoutAnyCredential() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "http://localhost:11434/v1/chat/completions", null).orElseThrow();

        assertThat(probe.url()).isEqualTo("http://localhost:11434/v1/models");
        assertThat(probe.headers()).doesNotContainKey("Authorization");
    }
}
