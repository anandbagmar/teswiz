package com.znsio.teswiz.visual;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
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
    private static final Set<String> SUPPORTED_PROVIDERS =
            Set.of("openai", "azure-openai", "anthropic", "gemini", "vertex-gemini", "custom");
    private static final Set<String> PING_HEALTH_CHECK_PROVIDERS = Set.of("openai", "azure-openai", "custom");
    private static final String VERTEX_GEMINI_PROVIDER = "vertex-gemini";
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
        if (PING_HEALTH_CHECK_PROVIDERS.contains(provider)) {
            performHealthCheck();
        } else {
            LOGGER.info("Skipping /models health-check for provider '{}' (structural validation only).", provider);
        }
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

        // Vertex AI authenticates with GCP Application Default Credentials, not an API key env var.
        if (VERTEX_GEMINI_PROVIDER.equals(provider)) {
            return;
        }

        String apiKeyEnvName = Runner.getLlmVisionApiKeyEnvName();
        if (resolveApiKey(apiKeyEnvName) == null) {
            fail("The environment variable named by LLM_VISION_API_KEY_ENV ('" + apiKeyEnvName
                 + "') is not set or is empty. Set it in your environment / CI secrets "
                 + "(the key value must never be stored in a config file).");
        }
    }

    private static void performHealthCheck() {
        String endpoint = Runner.getLlmVisionEndpoint();
        String modelsUrl = deriveModelsUrl(endpoint);
        String apiKey = resolveApiKey(Runner.getLlmVisionApiKeyEnvName());

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);

        TeswizApiResponse response;
        try {
            response = ApiService.get(modelsUrl, null, headers);
        } catch (RuntimeException e) {
            fail("LLM vision health-check could not reach '" + modelsUrl + "': " + e.getMessage()
                 + ". Verify LLM_VISION_ENDPOINT and network access.");
            return;
        }

        int status = response.getStatusCode();
        if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
            fail("LLM vision health-check was rejected with HTTP " + status
                 + " (invalid or unauthorized API key). Verify the key referenced by LLM_VISION_API_KEY_ENV.");
        }
        if (status < 200 || status >= 300) {
            fail("LLM vision health-check against '" + modelsUrl + "' returned HTTP " + status
                 + ". Verify LLM_VISION_ENDPOINT and that the key is authorized.");
        }
        LOGGER.info("LLM vision health-check passed (HTTP {} from {}).", status, modelsUrl);
    }

    private static String deriveModelsUrl(String endpoint) {
        // chat completions endpoints typically live under .../v1/chat/completions; the sibling
        // .../v1/models endpoint is the cheapest authorization probe.
        String normalized = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        int chatIndex = normalized.indexOf("/chat/completions");
        if (chatIndex > 0) {
            return normalized.substring(0, chatIndex) + "/models";
        }
        int completionsIndex = normalized.indexOf("/completions");
        if (completionsIndex > 0) {
            return normalized.substring(0, completionsIndex) + "/models";
        }
        return normalized + "/models";
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
