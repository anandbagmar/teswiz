package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class AzureOpenAiLlmVisionProviderTest {

    private static final byte[] SCREENSHOT = {1, 2, 3, 4};

    private final AzureOpenAiLlmVisionProvider provider = new AzureOpenAiLlmVisionProvider("2024-06-01");

    @Test
    void buildHeaders_usesApiKeyHeaderNotBearer() {
        Map<String, String> headers = provider.buildHeaders("azure-secret");

        assertThat(headers).containsEntry("api-key", "azure-secret");
        assertThat(headers).doesNotContainKey("Authorization");
        assertThat(headers.get("content-type")).containsIgnoringCase("application/json");
    }

    @Test
    void resolveEndpoint_appendsApiVersionQueryWhenAbsent() {
        String endpoint = "https://my.openai.azure.com/openai/deployments/gpt4o/chat/completions";

        assertThat(provider.resolveEndpoint(endpoint, "gpt-4o"))
                .isEqualTo(endpoint + "?api-version=2024-06-01");
    }

    @Test
    void resolveEndpoint_preservesExistingQueryStringAndAppendsApiVersion() {
        String endpoint = "https://my.openai.azure.com/openai/deployments/gpt4o/chat/completions?foo=bar";

        assertThat(provider.resolveEndpoint(endpoint, "gpt-4o"))
                .isEqualTo(endpoint + "&api-version=2024-06-01");
    }

    @Test
    void resolveEndpoint_doesNotDuplicateApiVersionWhenAlreadyPresent() {
        String endpoint = "https://my.openai.azure.com/openai/deployments/gpt4o/chat/completions?api-version=2023-01-01";

        assertThat(provider.resolveEndpoint(endpoint, "gpt-4o")).isEqualTo(endpoint);
    }

    @Test
    void resolveEndpoint_derivesDeploymentPathFromBaseUrlAndModel() {
        String base = "https://my.openai.azure.com/";

        assertThat(provider.resolveEndpoint(base, "gpt-5.4"))
                .isEqualTo("https://my.openai.azure.com/openai/deployments/gpt-5.4/chat/completions?api-version=2024-06-01");
    }

    @Test
    void resolveEndpoint_derivesDeploymentPathFromBaseUrlWithoutTrailingSlash() {
        String base = "https://my.openai.azure.com";

        assertThat(provider.resolveEndpoint(base, "gpt-5.4"))
                .isEqualTo("https://my.openai.azure.com/openai/deployments/gpt-5.4/chat/completions?api-version=2024-06-01");
    }

    @Test
    void resolveEndpoint_leavesExplicitDeploymentPathUntouchedEvenWhenModelDiffers() {
        // A full deployment path must win over derivation (deployment name may differ from model id).
        String fullPath = "https://my.openai.azure.com/openai/deployments/my-custom-deployment/chat/completions";

        assertThat(provider.resolveEndpoint(fullPath, "gpt-5.4"))
                .isEqualTo(fullPath + "?api-version=2024-06-01");
    }

    @Test
    void healthCheckProbe_derivesModelsListingWithApiVersionAndApiKeyHeader() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "https://my.openai.azure.com/openai/deployments/gpt4o/chat/completions", "azure-secret").orElseThrow();

        assertThat(probe.url()).isEqualTo("https://my.openai.azure.com/openai/models?api-version=2024-06-01");
        assertThat(probe.headers()).containsEntry("api-key", "azure-secret");
        assertThat(probe.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void buildRequestBody_matchesOpenAiWireFormat() {
        JSONObject body = new JSONObject(
                provider.buildRequestBody("system-prompt", "find it", SCREENSHOT, List.of(), "gpt-4o"));

        assertThat(body.getString("model")).isEqualTo("gpt-4o");
        assertThat(body.getInt("temperature")).isZero();
        JSONArray messages = body.getJSONArray("messages");
        assertThat(messages.getJSONObject(0).getString("role")).isEqualTo("system");
        assertThat(countImageParts(body)).isEqualTo(1);
        assertThat(body.toString()).contains("data:image/png;base64,");
    }

    @Test
    void parseResponse_extractsContentAndTokenUsage() {
        String responseBody = new JSONObject()
                .put("choices", new JSONArray().put(new JSONObject()
                        .put("message", new JSONObject().put("content", "answer"))))
                .put("usage", new JSONObject()
                        .put("prompt_tokens", 5).put("completion_tokens", 6).put("total_tokens", 11))
                .toString();

        ParsedLlmResponse parsed = provider.parseResponse(responseBody);

        assertThat(parsed.content()).isEqualTo("answer");
        assertThat(parsed.promptTokens()).isEqualTo(5);
        assertThat(parsed.completionTokens()).isEqualTo(6);
        assertThat(parsed.totalTokens()).isEqualTo(11);
    }

    @Test
    void healthCheckProbe_derivesResourceRootModelsListingFromBareBaseUrl() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "https://my.openai.azure.com/", "azure-secret").orElseThrow();
        assertThat(probe.url())
                .isEqualTo("https://my.openai.azure.com/openai/models?api-version=2024-06-01")
                .doesNotContain("/deployments/");
    }

    @Test
    void healthCheckProbe_derivesResourceRootModelsListingNotDeploymentScoped() {
        LlmVisionHealthCheckProbe probe = provider.buildHealthCheckProbe(
                "https://my.openai.azure.com/openai/deployments/gpt4o/chat/completions", "azure-secret").orElseThrow();
        assertThat(probe.url())
                .isEqualTo("https://my.openai.azure.com/openai/models?api-version=2024-06-01")
                .doesNotContain("/deployments/");
    }

    private int countImageParts(JSONObject body) {
        JSONArray content = body.getJSONArray("messages").getJSONObject(1).getJSONArray("content");
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if ("image_url".equals(content.getJSONObject(i).optString("type"))) {
                count++;
            }
        }
        return count;
    }
}
