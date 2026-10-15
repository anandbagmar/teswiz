package com.znsio.teswiz.visual;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Google Vertex AI Gemini provider. The request body and response parsing are inherited from
 * {@link GeminiLlmVisionProvider}; only authorization (an OAuth2 {@code Bearer} token from a
 * {@link GcpAccessTokenProvider}) and endpoint resolution (ensure {@code :generateContent}, with no
 * {@code ?key=} query) differ.
 */
public class VertexGeminiLlmVisionProvider extends GeminiLlmVisionProvider {

    private final GcpAccessTokenProvider accessTokenProvider;

    public VertexGeminiLlmVisionProvider(GcpAccessTokenProvider accessTokenProvider) {
        super("");
        this.accessTokenProvider = accessTokenProvider;
    }

    @Override
    public String resolveEndpoint(String configuredEndpoint, String model) {
        return ensureGenerateContent(configuredEndpoint);
    }

    @Override
    public Map<String, String> buildHeaders(String apiKeyOrToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + apiKeyOrToken);
        headers.put("content-type", "application/json");
        return headers;
    }

    /**
     * Returns the current OAuth2 access token value, obtained lazily from the configured
     * {@link GcpAccessTokenProvider}. {@link LlmVisionService} passes this value to
     * {@link #buildHeaders(String)}.
     */
    public String resolveAccessToken() {
        return accessTokenProvider.getAccessToken();
    }

    /**
     * Vertex authenticates with a GCP OAuth2 bearer token (not a {@code ?key=} query), so the probe
     * mints a token from the configured {@link GcpAccessTokenProvider} - failing fast if Application
     * Default Credentials are unavailable - and lists the sibling {@code /models} collection with a
     * {@code Bearer} header. The ignored {@code apiKeyOrToken} argument reflects that Vertex sources
     * its credential from ADC rather than an API-key env var.
     */
    @Override
    public Optional<LlmVisionHealthCheckProbe> buildHealthCheckProbe(String configuredEndpoint,
                                                                     String apiKeyOrToken) {
        String token = resolveAccessToken();
        return Optional.of(new LlmVisionHealthCheckProbe(
                deriveModelsBaseUrl(configuredEndpoint), buildHeaders(token)));
    }
}
