package com.znsio.teswiz.visual;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Azure OpenAI provider. The request body and response parsing are identical to OpenAI; only the
 * authorization header ({@code api-key} rather than {@code Bearer}) and the endpoint (which must
 * carry an {@code api-version} query parameter) differ.
 */
public class AzureOpenAiLlmVisionProvider extends OpenAiLlmVisionProvider {

    private static final String API_VERSION_PARAM = "api-version";

    private final String apiVersion;

    public AzureOpenAiLlmVisionProvider(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    @Override
    public String resolveEndpoint(String configuredEndpoint, String model) {
        if (configuredEndpoint.contains(API_VERSION_PARAM + "=")) {
            return configuredEndpoint;
        }
        String separator = configuredEndpoint.contains("?") ? "&" : "?";
        return configuredEndpoint + separator + API_VERSION_PARAM + "=" + apiVersion;
    }

    @Override
    public Map<String, String> buildHeaders(String apiKeyOrToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("api-key", apiKeyOrToken);
        headers.put("content-type", "application/json");
        return headers;
    }
}
