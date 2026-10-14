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
 * OpenAI-compatible chat-completions provider, also used for {@code custom} OpenAI-compatible
 * endpoints. Authorization is a {@code Bearer} token; the request body is a {@code system} message
 * followed by a {@code user} message carrying the user text, the screenshot {@code image_url} part,
 * then one {@code image_url} part per reference image.
 */
public class OpenAiLlmVisionProvider implements LlmVisionProvider {

    private static final Logger LOGGER = LogManager.getLogger(OpenAiLlmVisionProvider.class.getName());

    @Override
    public String resolveEndpoint(String configuredEndpoint, String model) {
        return configuredEndpoint;
    }

    @Override
    public Map<String, String> buildHeaders(String apiKeyOrToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + apiKeyOrToken);
        headers.put("content-type", "application/json");
        return headers;
    }

    @Override
    public String buildRequestBody(String systemPrompt, String userText, byte[] screenshotPng,
                                   List<byte[]> referenceImagesPng, String model) {
        JSONArray userContent = new JSONArray();
        userContent.put(textPart(userText));
        userContent.put(imagePart(screenshotPng));
        for (byte[] reference : referenceImagesPng) {
            userContent.put(imagePart(reference));
        }

        JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "system").put("content", systemPrompt))
                .put(new JSONObject().put("role", "user").put("content", userContent));

        return new JSONObject()
                .put("model", model)
                .put("temperature", 0)
                .put("messages", messages)
                .toString();
    }

    @Override
    public ParsedLlmResponse parseResponse(String responseBody) {
        String content = extractContent(responseBody);
        JSONObject usage = optUsage(responseBody);
        long promptTokens = usage == null ? 0 : usage.optLong("prompt_tokens", 0);
        long completionTokens = usage == null ? 0 : usage.optLong("completion_tokens", 0);
        long totalTokens = usage == null ? 0 : usage.optLong("total_tokens", 0);
        return new ParsedLlmResponse(content, promptTokens, completionTokens, totalTokens);
    }

    private String extractContent(String responseBody) {
        try {
            JSONArray choices = new JSONObject(responseBody).optJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            return message == null ? null : message.optString("content", null);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not parse OpenAI vision response envelope: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject optUsage(String responseBody) {
        try {
            return new JSONObject(responseBody).optJSONObject("usage");
        } catch (RuntimeException e) {
            LOGGER.debug("No usage block in OpenAI vision response: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject textPart(String text) {
        return new JSONObject().put("type", "text").put("text", text);
    }

    private JSONObject imagePart(byte[] imageBytes) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return new JSONObject().put("type", "image_url")
                .put("image_url", new JSONObject().put("url", "data:image/png;base64," + base64));
    }
}
