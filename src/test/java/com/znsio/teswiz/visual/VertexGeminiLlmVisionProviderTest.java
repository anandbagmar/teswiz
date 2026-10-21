package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class VertexGeminiLlmVisionProviderTest {

    private static final byte[] SCREENSHOT = {1, 2, 3, 4};

    private final VertexGeminiLlmVisionProvider provider =
            new VertexGeminiLlmVisionProvider(() -> "test-token");

    @Test
    void buildHeaders_usesBearerOauthTokenAndJsonContentType() {
        Map<String, String> headers = provider.buildHeaders("test-token");

        assertThat(headers).containsEntry("Authorization", "Bearer test-token");
        assertThat(headers.get("content-type")).containsIgnoringCase("application/json");
    }

    @Test
    void resolveEndpoint_appendsGenerateContentWithoutApiKeyQuery() {
        String endpoint = "https://us-central1-aiplatform.googleapis.com/v1/projects/p/locations/us-central1/"
                + "publishers/google/models/gemini-1.5-flash";

        assertThat(provider.resolveEndpoint(endpoint, "gemini-1.5-flash"))
                .isEqualTo(endpoint + ":generateContent");
    }

    @Test
    void resolveEndpoint_leavesExistingGenerateContentUnchangedAndAddsNoKey() {
        String endpoint = "https://us-central1-aiplatform.googleapis.com/v1/projects/p/locations/us-central1/"
                + "publishers/google/models/gemini-1.5-flash:generateContent";

        String resolved = provider.resolveEndpoint(endpoint, "gemini-1.5-flash");

        assertThat(resolved).isEqualTo(endpoint);
        assertThat(resolved).doesNotContain("key=");
    }

    @Test
    void buildRequestBody_matchesGeminiWireFormat() {
        JSONObject body = new JSONObject(
                provider.buildRequestBody("system-prompt", "find it", SCREENSHOT, List.of(), "gemini-1.5-flash"));

        assertThat(body.getJSONObject("systemInstruction").getJSONArray("parts")
                .getJSONObject(0).getString("text")).isEqualTo("system-prompt");
        assertThat(body.getJSONObject("generationConfig").getInt("temperature")).isZero();
        assertThat(countInlineImages(body)).isEqualTo(1);
        assertThat(body.toString()).contains("inlineData");
    }

    @Test
    void healthCheckProbe_listsModelsCollectionWithBearerTokenAndNoKeyQuery() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "https://us-central1-aiplatform.googleapis.com/v1/projects/p/locations/us-central1/"
                        + "publishers/google/models/gemini-1.5-flash:generateContent", "ignored").orElseThrow();

        assertThat(probe.url()).isEqualTo(
                "https://us-central1-aiplatform.googleapis.com/v1/projects/p/locations/us-central1/"
                        + "publishers/google/models");
        assertThat(probe.url()).doesNotContain("key=");
        assertThat(probe.headers()).containsEntry("Authorization", "Bearer test-token");
    }

    @Test
    void parseResponse_concatenatesCandidatePartsAndMapsUsageMetadata() {
        String responseBody = new JSONObject()
                .put("candidates", new JSONArray().put(new JSONObject()
                        .put("content", new JSONObject().put("parts", new JSONArray()
                                .put(new JSONObject().put("text", "answer"))))))
                .put("usageMetadata", new JSONObject()
                        .put("promptTokenCount", 12)
                        .put("candidatesTokenCount", 3)
                        .put("totalTokenCount", 15))
                .toString();

        ParsedLlmResponse parsed = provider.parseResponse(responseBody);

        assertThat(parsed.content()).isEqualTo("answer");
        assertThat(parsed.promptTokens()).isEqualTo(12);
        assertThat(parsed.completionTokens()).isEqualTo(3);
        assertThat(parsed.totalTokens()).isEqualTo(15);
    }

    private int countInlineImages(JSONObject body) {
        JSONArray parts = body.getJSONArray("contents").getJSONObject(0).getJSONArray("parts");
        int count = 0;
        for (int i = 0; i < parts.length(); i++) {
            if (parts.getJSONObject(i).has("inlineData")) {
                count++;
            }
        }
        return count;
    }
}
