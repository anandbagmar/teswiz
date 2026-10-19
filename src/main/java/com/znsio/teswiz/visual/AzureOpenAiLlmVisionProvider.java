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
        return appendApiVersion(resolveDeploymentPath(configuredEndpoint, model));
    }

    /**
     * Azure chat-completions live at {@code <base>/openai/deployments/<deployment>/chat/completions}.
     * If the configured endpoint already contains a deployment path it is used as-is (its deployment
     * name may legitimately differ from the model id); otherwise the full path is derived from the
     * base resource URL and {@code LLM_VISION_MODEL} (used as the deployment name).
     */
    private String resolveDeploymentPath(String configuredEndpoint, String model) {
        if (configuredEndpoint.contains("/openai/deployments/")) {
            return configuredEndpoint;
        }
        String base = configuredEndpoint.endsWith("/")
                ? configuredEndpoint.substring(0, configuredEndpoint.length() - 1)
                : configuredEndpoint;
        return base + "/openai/deployments/" + model + "/chat/completions";
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

    @Override
    protected String deriveModelsUrl(String configuredEndpoint) {
        // Azure model listing is at the resource root (/openai/models), not under
        // /openai/deployments/<deployment>/ where chat/completions lives; the OpenAI base
        // derivation would yield .../deployments/<deployment>/models which Azure 404s.
        int i = configuredEndpoint.indexOf("/openai/");
        if (i > 0) {
            return configuredEndpoint.substring(0, i) + "/openai/models";
        }
        // Bare base resource URL (no /openai/ segment): the models listing is <base>/openai/models.
        String base = configuredEndpoint.endsWith("/")
                ? configuredEndpoint.substring(0, configuredEndpoint.length() - 1)
                : configuredEndpoint;
        return base + "/openai/models";
    }

    private String appendApiVersion(String url) {
        if (url.contains(API_VERSION_PARAM + "=")) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + API_VERSION_PARAM + "=" + apiVersion;
    }
}
