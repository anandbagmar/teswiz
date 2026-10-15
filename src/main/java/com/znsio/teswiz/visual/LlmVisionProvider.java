package com.znsio.teswiz.visual;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Seam isolating the four provider-specific concerns of an LLM vision call: endpoint rewriting,
 * authorization headers, request-body wire format, and response parsing. {@link LlmVisionService}
 * owns everything else (retries, timeout, POST, traffic recording, usage accumulation, bounding-box
 * extraction, bounds-check, viewport scaling) and delegates only these four methods to the selected
 * provider.
 */
public interface LlmVisionProvider {

    /**
     * Returns the final request URL. Providers may rewrite the configured endpoint (append a query
     * string, normalise a path segment, etc.) based on the configured endpoint and model.
     */
    String resolveEndpoint(String configuredEndpoint, String model);

    /**
     * Returns the HTTP headers for the call, including the provider's authorization header shaped
     * from the supplied API key or OAuth token.
     */
    Map<String, String> buildHeaders(String apiKeyOrToken);

    /**
     * Builds the provider-specific JSON request body carrying the system prompt, the user text, the
     * screenshot PNG to search, and any reference-image PNGs.
     */
    String buildRequestBody(String systemPrompt, String userText, byte[] screenshotPng,
                            List<byte[]> referenceImagesPng, String model);

    /**
     * Parses the raw response body into a provider-agnostic {@link ParsedLlmResponse}.
     */
    ParsedLlmResponse parseResponse(String responseBody);

    /**
     * Returns a non-billable, authorization-only startup probe (typically a {@code GET /models}
     * listing) that {@link LlmVisionConfigValidator} runs to fail fast against an unreachable
     * endpoint or an invalid API key. Returns {@link Optional#empty()} when the provider has no
     * non-billable probe (its only endpoints are billable inference calls), in which case the
     * validator performs structural validation only.
     *
     * <p>The default is {@link Optional#empty()} (structural-only); providers with a safe probe
     * override this.
     *
     * @param configuredEndpoint the configured {@code LLM_VISION_ENDPOINT}
     * @param apiKeyOrToken      the resolved API key or OAuth token (may be {@code null} for a
     *                           credential-less local provider such as Ollama)
     */
    default Optional<LlmVisionHealthCheckProbe> buildHealthCheckProbe(String configuredEndpoint,
                                                                      String apiKeyOrToken) {
        return Optional.empty();
    }
}
