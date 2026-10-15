package com.znsio.teswiz.visual;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

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
        return appendApiVersion(configuredEndpoint);
    }

    @Override
    public Map<String, String> buildHeaders(String apiKeyOrToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("api-key", apiKeyOrToken);
        headers.put("content-type", "application/json");
        return headers;
    }

    /**
     * The Azure models listing also requires the {@code api-version} query parameter and the
     * {@code api-key} header, so the derived {@code /models} URL is decorated the same way as the
     * chat-completions endpoint.
     */
    @Override
    public Optional<LlmVisionHealthCheckProbe> buildHealthCheckProbe(String configuredEndpoint,
                                                                     String apiKeyOrToken) {
        String modelsUrl = appendApiVersion(deriveModelsUrl(configuredEndpoint));
        return Optional.of(new LlmVisionHealthCheckProbe(modelsUrl, buildHeaders(apiKeyOrToken)));
    }

    private String appendApiVersion(String url) {
        if (url.contains(API_VERSION_PARAM + "=")) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + API_VERSION_PARAM + "=" + apiVersion;
    }
}
