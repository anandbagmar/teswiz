package com.znsio.teswiz.visual;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Google Gemini (AI Studio) provider. The API key is supplied as a {@code ?key=} query parameter on
 * the endpoint (not a header), and the body uses the {@code generateContent} wire format:
 * {@code systemInstruction}, a single {@code user} {@code contents} entry whose {@code parts} are
 * the user text, the screenshot {@code inlineData}, then any reference {@code inlineData} parts, and
 * {@code generationConfig.temperature=0}. The response's candidate {@code parts[].text} entries are
 * concatenated and {@code usageMetadata} supplies token counts.
 */
public class GeminiLlmVisionProvider implements LlmVisionProvider {

    private static final Logger LOGGER = LogManager.getLogger(GeminiLlmVisionProvider.class.getName());
    private static final String GENERATE_CONTENT_SUFFIX = ":generateContent";

    private final String apiKey;

    public GeminiLlmVisionProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String resolveEndpoint(String configuredEndpoint, String model) {
        String endpoint = ensureGenerateContent(configuredEndpoint);
        String separator = endpoint.contains("?") ? "&" : "?";
        return endpoint + separator + "key=" + apiKey;
    }

    /**
     * Ensures the endpoint path ends with {@code :generateContent}, appending it when the endpoint
     * references a model path but lacks the action suffix. The suffix is inserted before any query
     * string so it attaches to the path rather than the query.
     */
    protected String ensureGenerateContent(String configuredEndpoint) {
        int queryIndex = configuredEndpoint.indexOf('?');
        String path = queryIndex < 0 ? configuredEndpoint : configuredEndpoint.substring(0, queryIndex);
        String query = queryIndex < 0 ? "" : configuredEndpoint.substring(queryIndex);
        if (!path.contains(GENERATE_CONTENT_SUFFIX) && path.contains("/models/")) {
            path = path + GENERATE_CONTENT_SUFFIX;
        }
        return path + query;
    }

    @Override
    public Map<String, String> buildHeaders(String apiKeyOrToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("content-type", "application/json");
        return headers;
    }

    @Override
    public String buildRequestBody(String systemPrompt, String userText, byte[] screenshotPng,
                                   List<byte[]> referenceImagesPng, String model) {
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", userText));
        parts.put(inlineImagePart(screenshotPng));

        int referenceCount = referenceImagesPng.size();
        for (int i = 0; i < referenceCount; i++) {
            parts.put(new JSONObject().put("text", String.format(
                    "REFERENCE EXAMPLE %d of %d (what the target element looks like; NOT part of the screenshot):",
                    i + 1, referenceCount)));
            parts.put(inlineImagePart(referenceImagesPng.get(i)));
        }

        JSONObject systemInstruction = new JSONObject()
                .put("parts", new JSONArray().put(new JSONObject().put("text", systemPrompt)));
        JSONArray contents = new JSONArray()
                .put(new JSONObject().put("role", "user").put("parts", parts));

        return new JSONObject()
                .put("systemInstruction", systemInstruction)
                .put("contents", contents)
                .put("generationConfig", new JSONObject().put("temperature", 0))
                .toString();
    }

    @Override
    public ParsedLlmResponse parseResponse(String responseBody) {
        String content = extractContent(responseBody);
        JSONObject usage = optUsageMetadata(responseBody);
        long promptTokens = usage == null ? 0 : usage.optLong("promptTokenCount", 0);
        long completionTokens = usage == null ? 0 : usage.optLong("candidatesTokenCount", 0);
        long totalTokens = usage == null ? 0 : usage.optLong("totalTokenCount", 0);
        return new ParsedLlmResponse(content, promptTokens, completionTokens, totalTokens);
    }

    private String extractContent(String responseBody) {
        try {
            JSONArray candidates = new JSONObject(responseBody).optJSONArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            JSONObject candidateContent = candidates.getJSONObject(0).optJSONObject("content");
            if (candidateContent == null) {
                return null;
            }
            JSONArray parts = candidateContent.optJSONArray("parts");
            if (parts == null) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                text.append(parts.getJSONObject(i).optString("text", ""));
            }
            return text.toString();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not parse Gemini vision response envelope: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject optUsageMetadata(String responseBody) {
        try {
            return new JSONObject(responseBody).optJSONObject("usageMetadata");
        } catch (RuntimeException e) {
            LOGGER.debug("No usageMetadata block in Gemini vision response: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject inlineImagePart(byte[] imageBytes) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        JSONObject inlineData = new JSONObject().put("mimeType", "image/png").put("data", base64);
        return new JSONObject().put("inlineData", inlineData);
    }
}
