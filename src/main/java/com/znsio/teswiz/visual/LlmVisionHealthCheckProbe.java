package com.znsio.teswiz.visual;

import java.util.Map;

/**
 * A non-billable, authorization-only health-check request a {@link LlmVisionProvider} exposes so
 * {@link LlmVisionConfigValidator} can fail fast at startup against a broken endpoint or an invalid
 * API key - without ever invoking a billable model inference.
 *
 * <p>A probe is always a lightweight {@code GET} (for example a {@code /models} listing) whose only
 * purpose is to confirm the endpoint is reachable and the credential is accepted. Providers that
 * have no non-billable probe (for example Anthropic Messages and AWS Bedrock Runtime, whose only
 * endpoints are billable inference calls) return {@link java.util.Optional#empty()} from
 * {@link LlmVisionProvider#buildHealthCheckProbe(String, String)} to request structural validation
 * only.
 *
 * @param url     the fully-resolved {@code GET} URL to probe (may already embed an API key in the
 *                query string for providers that authenticate that way, e.g. Gemini AI Studio)
 * @param headers the request headers, including any authorization header; never {@code null}
 */
public record LlmVisionHealthCheckProbe(String url, Map<String, String> headers) {

    public LlmVisionHealthCheckProbe {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Health-check probe url must not be blank");
        }
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
