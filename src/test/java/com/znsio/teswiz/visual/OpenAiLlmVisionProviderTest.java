package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class OpenAiLlmVisionProviderTest {

    private static final byte[] SCREENSHOT = {1, 2, 3, 4};
    private static final byte[] REFERENCE = {9, 8, 7};

    private final OpenAiLlmVisionProvider provider = new OpenAiLlmVisionProvider();

    @Test
    void resolveEndpoint_leavesConfiguredEndpointUnchanged() {
        String endpoint = "https://api.openai.com/v1/chat/completions";

        assertThat(provider.resolveEndpoint(endpoint, "gpt-4o")).isEqualTo(endpoint);
    }

    @Test
    void healthCheckProbe_derivesModelsListingSiblingOfChatCompletions() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "https://api.openai.com/v1/chat/completions", "sk-secret").orElseThrow();

        assertThat(probe.url()).isEqualTo("https://api.openai.com/v1/models");
        assertThat(probe.headers()).containsEntry("Authorization", "Bearer sk-secret");
    }

    @Test
    void buildHeaders_usesBearerAuthorizationAndJsonContentType() {
        Map<String, String> headers = provider.buildHeaders("sk-secret");

        assertThat(headers).containsEntry("Authorization", "Bearer sk-secret");
        assertThat(headers.get("content-type")).containsIgnoringCase("application/json");
    }

    @Test
    void buildRequestBody_includesModelTemperatureAndSystemMessage() {
        JSONObject body = new JSONObject(
                provider.buildRequestBody("system-prompt", "find it", SCREENSHOT, List.of(), "gpt-4o"));

        assertThat(body.getString("model")).isEqualTo("gpt-4o");
        assertThat(body.getInt("temperature")).isZero();
        JSONObject systemMessage = body.getJSONArray("messages").getJSONObject(0);
        assertThat(systemMessage.getString("role")).isEqualTo("system");
        assertThat(systemMessage.getString("content")).isEqualTo("system-prompt");
    }

    @Test
    void buildRequestBody_hasOneImagePartWhenNoReferences() {
        assertThat(countImageParts(provider.buildRequestBody("sys", "txt", SCREENSHOT, List.of(), "gpt-4o")))
                .isEqualTo(1);
    }

    @Test
    void buildRequestBody_hasOneImagePartPerReferencePlusScreenshot() {
        String json = provider.buildRequestBody("sys", "txt", SCREENSHOT, List.of(REFERENCE, REFERENCE), "gpt-4o");

        assertThat(countImageParts(json)).isEqualTo(3);
    }

    @Test
    void buildRequestBody_encodesScreenshotAsBase64DataUrl() {
        String json = provider.buildRequestBody("sys", "txt", SCREENSHOT, List.of(), "gpt-4o");

        assertThat(json).contains("data:image/png;base64,");
    }

    @Test
    void parseResponse_extractsContentAndTokenUsage() {
        String responseBody = new JSONObject()
                .put("choices", new JSONArray().put(new JSONObject()
                        .put("message", new JSONObject().put("content", "{\"found\":false}"))))
                .put("usage", new JSONObject()
                        .put("prompt_tokens", 11).put("completion_tokens", 7).put("total_tokens", 18))
                .toString();

        ParsedLlmResponse parsed = provider.parseResponse(responseBody);

        assertThat(parsed.content()).isEqualTo("{\"found\":false}");
        assertThat(parsed.promptTokens()).isEqualTo(11);
        assertThat(parsed.completionTokens()).isEqualTo(7);
        assertThat(parsed.totalTokens()).isEqualTo(18);
    }

    private int countImageParts(String requestBodyJson) {
        JSONArray content = new JSONObject(requestBodyJson).getJSONArray("messages")
                .getJSONObject(1).getJSONArray("content");
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if ("image_url".equals(content.getJSONObject(i).optString("type"))) {
                count++;
            }
        }
        return count;
    }
}
