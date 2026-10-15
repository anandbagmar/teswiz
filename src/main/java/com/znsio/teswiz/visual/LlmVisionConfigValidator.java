package com.znsio.teswiz.visual;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.exceptions.InvalidTestDataException;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.services.ApiService;

/**
 * Fail-fast, startup-time validation for the LLM vision fallback. Runs only when
 * {@code LLM_VISION_FALLBACK_ENABLED=true}. It performs:
 *
 * <ol>
 *   <li>Local structural checks (endpoint present + well-formed URL, API key env var present and
 *       non-empty, provider recognised); and</li>
 *   <li>A lightweight authorization health-check (a {@code GET /models}-style ping) that fails fast
 *       on a definitive {@code 401/403} and on an unreachable endpoint.</li>
 * </ol>
 *
 * <p>Misconfiguration throws {@link InvalidTestDataException} with an actionable, boxed message so
 * a run never starts against a broken LLM vision configuration. The API key value is never logged.
 */
public final class LlmVisionConfigValidator {

    private static final Logger LOGGER = LogManager.getLogger(LlmVisionConfigValidator.class.getName());
    private static final Set<String> SUPPORTED_PROVIDERS = Set.of(
            "openai", "azure-openai", "anthropic", "gemini", "vertex-gemini", "custom",
            "groq", "xai", "openrouter", "ollama", "bedrock");
    // Providers whose credential is NOT an API-key env var: Vertex uses GCP Application Default
    // Credentials; Ollama is a local server that needs no key.
    private static final Set<String> NO_API_KEY_ENV_PROVIDERS = Set.of("vertex-gemini", "ollama");
    // Providers for which LLM_VISION_MODEL is mandatory (no safe hardcoded default - model ids churn
    // or are endpoint-specific), so a blank model fails fast at startup.
    private static final Set<String> MODEL_REQUIRED_PROVIDERS =
            Set.of("groq", "xai", "openrouter", "ollama", "bedrock");
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;

    private LlmVisionConfigValidator() {
    }

    public static void validateIfEnabled() {
        if (!Runner.isLlmVisionFallbackEnabled()) {
            return;
        }
        LOGGER.info("LLM vision fallback is enabled; validating configuration...");
        String provider = Runner.getLlmVisionProvider().toLowerCase();
        validateStructuralConfig(provider);
        performHealthCheck(provider);
        LOGGER.info("LLM vision fallback configuration validated successfully.");
    }

    private static void validateStructuralConfig(String provider) {
        if (!SUPPORTED_PROVIDERS.contains(provider)) {
            fail("Unsupported LLM_VISION_PROVIDER '" + provider + "'. Supported values: " + SUPPORTED_PROVIDERS + ".");
        }

        String endpoint = Runner.getLlmVisionEndpoint();
        if (endpoint.isBlank()) {
            fail("LLM_VISION_ENDPOINT is mandatory when LLM_VISION_FALLBACK_ENABLED=true, but it is blank.");
        }
        if (!isWellFormedUrl(endpoint)) {
            fail("LLM_VISION_ENDPOINT '" + endpoint + "' is not a well-formed http(s) URL.");
        }

        if (MODEL_REQUIRED_PROVIDERS.contains(provider) && Runner.getLlmVisionModel().isBlank()) {
            fail("LLM_VISION_MODEL is mandatory for LLM_VISION_PROVIDER '" + provider
                 + "' (no default model is assumed), but it is blank. Set it to a vision-capable model id.");
        }

        if (NO_API_KEY_ENV_PROVIDERS.contains(provider)) {
            return;
        }

        String apiKeyEnvName = Runner.getLlmVisionApiKeyEnvName();
        if (resolveApiKey(apiKeyEnvName) == null) {
            fail("The environment variable named by LLM_VISION_API_KEY_ENV ('" + apiKeyEnvName
                 + "') is not set or is empty. Set it in your environment / CI secrets "
                 + "(the key value must never be stored in a config file).");
        }
    }

    /**
     * Runs the provider's non-billable authorization probe, if it exposes one. Providers whose only
     * endpoints are billable inference calls (Anthropic, Bedrock) return no probe and are validated
     * structurally only.
     */
    private static void performHealthCheck(String provider) {
        LlmVisionProvider visionProvider = LlmVisionProviderFactory.create(
                provider, Runner.getLlmVisionApiVersion(),
                resolveApiKey(Runner.getLlmVisionApiKeyEnvName()), new GoogleAdcAccessTokenProvider());

        Optional<LlmVisionHealthCheckProbe> maybeProbe;
        try {
            maybeProbe = visionProvider.buildHealthCheckProbe(
                    Runner.getLlmVisionEndpoint(), resolveApiKey(Runner.getLlmVisionApiKeyEnvName()));
        } catch (RuntimeException e) {
            fail("LLM vision health-check could not obtain credentials for provider '" + provider
                 + "': " + e.getMessage() + ". Verify the provider's credentials are available.");
            return;
        }

        if (maybeProbe.isEmpty()) {
            LOGGER.info("Provider '{}' has no non-billable health-check probe; structural validation only.", provider);
            return;
        }

        LlmVisionHealthCheckProbe probe = maybeProbe.get();
        TeswizApiResponse response;
        try {
            response = ApiService.get(probe.url(), null, probe.headers());
        } catch (RuntimeException e) {
            fail("LLM vision health-check could not reach '" + probe.url() + "': " + e.getMessage()
                 + ". Verify LLM_VISION_ENDPOINT and network access.");
            return;
        }

        int status = response.getStatusCode();
        if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
            fail("LLM vision health-check was rejected with HTTP " + status
                 + " (invalid or unauthorized API key). Verify the key referenced by LLM_VISION_API_KEY_ENV.");
        }
        if (status < 200 || status >= 300) {
            fail("LLM vision health-check against '" + probe.url() + "' returned HTTP " + status
                 + ". Verify LLM_VISION_ENDPOINT and that the key is authorized.");
        }
        LOGGER.info("LLM vision health-check passed (HTTP {} from {}).", status, probe.url());
    }

    private static boolean isWellFormedUrl(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                   && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String resolveApiKey(String envName) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(envName);
        }
        return (value == null || value.isBlank()) ? null : value;
    }

    private static void fail(String reason) {
        String message = System.lineSeparator()
            + "====================================================================================================" + System.lineSeparator()
            + " [teswiz] LLM Vision Fallback is misconfigured!" + System.lineSeparator()
            + "----------------------------------------------------------------------------------------------------" + System.lineSeparator()
            + " " + reason + System.lineSeparator()
            + System.lineSeparator()
            + " Required when LLM_VISION_FALLBACK_ENABLED=true:" + System.lineSeparator()
            + "   - LLM_VISION_ENDPOINT set to your vision chat-completions endpoint" + System.lineSeparator()
            + "   - the env var named by LLM_VISION_API_KEY_ENV set to a valid, authorized API key" + System.lineSeparator()
            + "====================================================================================================";
        throw new InvalidTestDataException(message);
    }
}
