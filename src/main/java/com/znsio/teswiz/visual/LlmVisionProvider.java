package com.znsio.teswiz.visual;

import java.util.List;
import java.util.Map;

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
}
