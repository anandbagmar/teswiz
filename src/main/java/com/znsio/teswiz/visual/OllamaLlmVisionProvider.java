package com.znsio.teswiz.visual;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ollama provider for a local or self-hosted server exposing Ollama's OpenAI-compatible
 * {@code /v1/chat/completions} endpoint. The request body and response parsing are inherited
 * unchanged from {@link OpenAiLlmVisionProvider}; only authorization differs - a local Ollama
 * server needs no API key, so no {@code Authorization} header is sent. Its non-billable
 * {@code /v1/models} listing (the sibling of {@code /v1/chat/completions}) is probed without any
 * credential.
 */
public class OllamaLlmVisionProvider extends OpenAiLlmVisionProvider {

    @Override
    public Map<String, String> buildHeaders(String apiKeyOrToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("content-type", "application/json");
        return headers;
    }

    @Override
    public Optional<LlmVisionHealthCheckProbe> buildHealthCheckProbe(String configuredEndpoint,
                                                                     String apiKeyOrToken) {
        return Optional.of(new LlmVisionHealthCheckProbe(
                deriveModelsUrl(configuredEndpoint), buildHeaders(apiKeyOrToken)));
    }
}
