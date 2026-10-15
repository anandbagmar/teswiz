package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class BedrockLlmVisionProviderTest {

    private static final byte[] SCREENSHOT = {1, 2, 3, 4};
    private static final byte[] REFERENCE = {9, 8, 7};

    private final BedrockLlmVisionProvider provider = new BedrockLlmVisionProvider();

    @Test
    void resolveEndpoint_leavesConfiguredConverseEndpointUnchanged() {
        String endpoint = "https://bedrock-runtime.us-east-1.amazonaws.com/model/anthropic.claude-3-5-sonnet/converse";

        assertThat(provider.resolveEndpoint(endpoint, "ignored")).isEqualTo(endpoint);
    }

    @Test
    void buildHeaders_usesBearerAuthorizationForBedrockApiKey() {
        assertThat(provider.buildHeaders("bedrock-api-key"))
                .containsEntry("Authorization", "Bearer bedrock-api-key");
    }

    @Test
    void buildRequestBody_usesConverseSchemaWithSystemAndImageBlocks() {
        JSONObject body = new JSONObject(
                provider.buildRequestBody("system-prompt", "find it", SCREENSHOT, List.of(REFERENCE), "model-id"));

        assertThat(body.getJSONArray("system").getJSONObject(0).getString("text")).isEqualTo("system-prompt");
        JSONArray content = body.getJSONArray("messages").getJSONObject(0).getJSONArray("content");
        assertThat(body.getJSONArray("messages").getJSONObject(0).getString("role")).isEqualTo("user");
        assertThat(countImageBlocks(content)).isEqualTo(2);
        assertThat(body.getJSONObject("inferenceConfig").getInt("temperature")).isZero();
    }

    @Test
    void parseResponse_extractsConverseOutputTextAndTokenUsage() {
        String responseBody = new JSONObject()
                .put("output", new JSONObject().put("message", new JSONObject()
                        .put("content", new JSONArray().put(new JSONObject().put("text", "{\"found\":false}")))))
                .put("usage", new JSONObject()
                        .put("inputTokens", 12).put("outputTokens", 4).put("totalTokens", 16))
                .toString();

        ParsedLlmResponse parsed = provider.parseResponse(responseBody);

        assertThat(parsed.content()).isEqualTo("{\"found\":false}");
        assertThat(parsed.promptTokens()).isEqualTo(12);
        assertThat(parsed.completionTokens()).isEqualTo(4);
        assertThat(parsed.totalTokens()).isEqualTo(16);
    }

    @Test
    void healthCheckProbe_isStructuralOnly() {
        assertThat(provider.buildHealthCheckProbe("https://bedrock-runtime.us-east-1.amazonaws.com", "k"))
                .isEmpty();
    }

    private int countImageBlocks(JSONArray content) {
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.getJSONObject(i).has("image")) {
                count++;
            }
        }
        return count;
    }
}
