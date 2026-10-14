package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class AnthropicLlmVisionProviderTest {

    private static final byte[] SCREENSHOT = {1, 2, 3, 4};
    private static final byte[] REFERENCE = {9, 8, 7};

    private final AnthropicLlmVisionProvider provider = new AnthropicLlmVisionProvider();

    @Test
    void resolveEndpoint_leavesConfiguredEndpointUnchanged() {
        String endpoint = "https://api.anthropic.com/v1/messages";

        assertThat(provider.resolveEndpoint(endpoint, "claude-3-5-sonnet")).isEqualTo(endpoint);
    }

    @Test
    void buildHeaders_usesXApiKeyAndAnthropicVersion() {
        Map<String, String> headers = provider.buildHeaders("anthropic-secret");

        assertThat(headers).containsEntry("x-api-key", "anthropic-secret");
        assertThat(headers).containsEntry("anthropic-version", "2023-06-01");
        assertThat(headers).doesNotContainKey("Authorization");
        assertThat(headers.get("content-type")).containsIgnoringCase("application/json");
    }

    @Test
    void buildRequestBody_placesSystemPromptAndModelAtTopLevel() {
        JSONObject body = new JSONObject(
                provider.buildRequestBody("system-prompt", "find it", SCREENSHOT, List.of(), "claude-3-5-sonnet"));

        assertThat(body.getString("model")).isEqualTo("claude-3-5-sonnet");
        assertThat(body.getString("system")).isEqualTo("system-prompt");
        assertThat(body.getInt("max_tokens")).isEqualTo(1024);
    }

    @Test
    void buildRequestBody_hasOneImageBlockWhenNoReferences() {
        assertThat(countImageBlocks(provider.buildRequestBody("sys", "txt", SCREENSHOT, List.of(), "claude")))
                .isEqualTo(1);
    }

    @Test
    void buildRequestBody_hasOneImageBlockPerReferencePlusScreenshot() {
        String json = provider.buildRequestBody("sys", "txt", SCREENSHOT, List.of(REFERENCE, REFERENCE), "claude");

        assertThat(countImageBlocks(json)).isEqualTo(3);
    }

    @Test
    void buildRequestBody_encodesScreenshotAsBase64WithPngMediaType() {
        JSONObject body = new JSONObject(provider.buildRequestBody("sys", "txt", SCREENSHOT, List.of(), "claude"));
        JSONObject firstImage = firstImageBlock(body);

        assertThat(firstImage.getJSONObject("source").getString("type")).isEqualTo("base64");
        assertThat(firstImage.getJSONObject("source").getString("media_type")).isEqualTo("image/png");
        assertThat(firstImage.getJSONObject("source").getString("data")).isNotEmpty();
    }

    @Test
    void parseResponse_concatenatesTextBlocksAndMapsTokenUsage() {
        String responseBody = new JSONObject()
                .put("content", new JSONArray()
                        .put(new JSONObject().put("type", "text").put("text", "{\"found\":"))
                        .put(new JSONObject().put("type", "text").put("text", "true}")))
                .put("usage", new JSONObject().put("input_tokens", 20).put("output_tokens", 4))
                .toString();

        ParsedLlmResponse parsed = provider.parseResponse(responseBody);

        assertThat(parsed.content()).isEqualTo("{\"found\":true}");
        assertThat(parsed.promptTokens()).isEqualTo(20);
        assertThat(parsed.completionTokens()).isEqualTo(4);
        assertThat(parsed.totalTokens()).isEqualTo(24);
    }

    private int countImageBlocks(String requestBodyJson) {
        JSONArray content = userContent(new JSONObject(requestBodyJson));
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if ("image".equals(content.getJSONObject(i).optString("type"))) {
                count++;
            }
        }
        return count;
    }

    private JSONObject firstImageBlock(JSONObject body) {
        JSONArray content = userContent(body);
        for (int i = 0; i < content.length(); i++) {
            if ("image".equals(content.getJSONObject(i).optString("type"))) {
                return content.getJSONObject(i);
            }
        }
        throw new AssertionError("no image block found");
    }

    private JSONArray userContent(JSONObject body) {
        return body.getJSONArray("messages").getJSONObject(0).getJSONArray("content");
    }
}
