package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class GeminiLlmVisionProviderTest {

    private static final byte[] SCREENSHOT = {1, 2, 3, 4};
    private static final byte[] REFERENCE = {9, 8, 7};

    private final GeminiLlmVisionProvider provider = new GeminiLlmVisionProvider("gemini-key");

    @Test
    void buildHeaders_onlySetsJsonContentTypeAndNoAuthHeader() {
        Map<String, String> headers = provider.buildHeaders("gemini-key");

        assertThat(headers.get("content-type")).containsIgnoringCase("application/json");
        assertThat(headers).doesNotContainKey("Authorization");
        assertThat(headers).doesNotContainKey("x-api-key");
    }

    @Test
    void resolveEndpoint_appendsGenerateContentAndApiKey() {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash";

        assertThat(provider.resolveEndpoint(endpoint, "gemini-1.5-flash"))
                .isEqualTo(endpoint + ":generateContent?key=gemini-key");
    }

    @Test
    void resolveEndpoint_preservesExistingGenerateContentAndQuery() {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?alt=json";

        assertThat(provider.resolveEndpoint(endpoint, "gemini-1.5-flash"))
                .isEqualTo(endpoint + "&key=gemini-key");
    }

    @Test
    void healthCheckProbe_listsModelsCollectionWithKeyInQueryNotHeader() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent",
                "gemini-key").orElseThrow();

        assertThat(probe.url()).isEqualTo(
                "https://generativelanguage.googleapis.com/v1beta/models?key=gemini-key");
        assertThat(probe.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void buildRequestBody_placesSystemInstructionAndTemperatureZero() {
        JSONObject body = new JSONObject(
                provider.buildRequestBody("system-prompt", "find it", SCREENSHOT, List.of(), "gemini-1.5-flash"));

        assertThat(body.getJSONObject("systemInstruction").getJSONArray("parts")
                .getJSONObject(0).getString("text")).isEqualTo("system-prompt");
        assertThat(body.getJSONObject("generationConfig").getInt("temperature")).isZero();
    }

    @Test
    void buildRequestBody_hasOneInlineImageWhenNoReferences() {
        assertThat(countInlineImages(provider.buildRequestBody("sys", "txt", SCREENSHOT, List.of(), "gemini")))
                .isEqualTo(1);
    }

    @Test
    void buildRequestBody_hasOneInlineImagePerReferencePlusScreenshot() {
        String json = provider.buildRequestBody("sys", "txt", SCREENSHOT, List.of(REFERENCE, REFERENCE), "gemini");

        assertThat(countInlineImages(json)).isEqualTo(3);
    }

    @Test
    void buildRequestBody_encodesScreenshotAsBase64PngInlineData() {
        JSONObject body = new JSONObject(provider.buildRequestBody("sys", "txt", SCREENSHOT, List.of(), "gemini"));
        JSONObject inlineData = firstInlineData(body);

        assertThat(inlineData.getString("mimeType")).isEqualTo("image/png");
        assertThat(inlineData.getString("data")).isNotEmpty();
    }

    @Test
    void parseResponse_concatenatesCandidatePartsAndMapsUsageMetadata() {
        String responseBody = new JSONObject()
                .put("candidates", new JSONArray().put(new JSONObject()
                        .put("content", new JSONObject().put("parts", new JSONArray()
                                .put(new JSONObject().put("text", "{\"found\":"))
                                .put(new JSONObject().put("text", "false}"))))))
                .put("usageMetadata", new JSONObject()
                        .put("promptTokenCount", 30)
                        .put("candidatesTokenCount", 9)
                        .put("totalTokenCount", 39))
                .toString();

        ParsedLlmResponse parsed = provider.parseResponse(responseBody);

        assertThat(parsed.content()).isEqualTo("{\"found\":false}");
        assertThat(parsed.promptTokens()).isEqualTo(30);
        assertThat(parsed.completionTokens()).isEqualTo(9);
        assertThat(parsed.totalTokens()).isEqualTo(39);
    }

    private int countInlineImages(String requestBodyJson) {
        JSONArray parts = userParts(new JSONObject(requestBodyJson));
        int count = 0;
        for (int i = 0; i < parts.length(); i++) {
            if (parts.getJSONObject(i).has("inlineData")) {
                count++;
            }
        }
        return count;
    }

    private JSONObject firstInlineData(JSONObject body) {
        JSONArray parts = userParts(body);
        for (int i = 0; i < parts.length(); i++) {
            if (parts.getJSONObject(i).has("inlineData")) {
                return parts.getJSONObject(i).getJSONObject("inlineData");
            }
        }
        throw new AssertionError("no inlineData part found");
    }

    private JSONArray userParts(JSONObject body) {
        return body.getJSONArray("contents").getJSONObject(0).getJSONArray("parts");
    }
}
