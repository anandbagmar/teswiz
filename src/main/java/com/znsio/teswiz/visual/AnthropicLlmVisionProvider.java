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
 * Anthropic Messages API provider. Authorization uses the {@code x-api-key} header plus the
 * {@code anthropic-version} header. The body carries the system prompt at the top level and a
 * single {@code user} message whose content is the user text followed by image blocks (screenshot
 * then any labelled reference images). The response's {@code content[]} text entries are
 * concatenated and token usage is mapped from {@code input_tokens}/{@code output_tokens}.
 */
public class AnthropicLlmVisionProvider implements LlmVisionProvider {

    private static final Logger LOGGER = LogManager.getLogger(AnthropicLlmVisionProvider.class.getName());
    private static final int MAX_TOKENS = 1024;
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Override
    public String resolveEndpoint(String configuredEndpoint, String model) {
        return configuredEndpoint;
    }

    @Override
    public Map<String, String> buildHeaders(String apiKeyOrToken) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-api-key", apiKeyOrToken);
        headers.put("anthropic-version", ANTHROPIC_VERSION);
        headers.put("content-type", "application/json");
        return headers;
    }

    @Override
    public String buildRequestBody(String systemPrompt, String userText, byte[] screenshotPng,
                                   List<byte[]> referenceImagesPng, String model) {
        JSONArray content = new JSONArray();
        content.put(textBlock(userText));
        content.put(imageBlock(screenshotPng));

        int referenceCount = referenceImagesPng.size();
        for (int i = 0; i < referenceCount; i++) {
            content.put(textBlock(String.format(
                    "REFERENCE EXAMPLE %d of %d (what the target element looks like; NOT part of the screenshot):",
                    i + 1, referenceCount)));
            content.put(imageBlock(referenceImagesPng.get(i)));
        }

        JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "user").put("content", content));

        return new JSONObject()
                .put("model", model)
                .put("max_tokens", MAX_TOKENS)
                .put("system", systemPrompt)
                .put("messages", messages)
                .toString();
    }

    @Override
    public ParsedLlmResponse parseResponse(String responseBody) {
        String content = extractContent(responseBody);
        JSONObject usage = optUsage(responseBody);
        long promptTokens = usage == null ? 0 : usage.optLong("input_tokens", 0);
        long completionTokens = usage == null ? 0 : usage.optLong("output_tokens", 0);
        return new ParsedLlmResponse(content, promptTokens, completionTokens, promptTokens + completionTokens);
    }

    private String extractContent(String responseBody) {
        try {
            JSONArray content = new JSONObject(responseBody).optJSONArray("content");
            if (content == null) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                JSONObject block = content.getJSONObject(i);
                if ("text".equals(block.optString("type"))) {
                    text.append(block.optString("text", ""));
                }
            }
            return text.toString();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not parse Anthropic vision response envelope: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject optUsage(String responseBody) {
        try {
            return new JSONObject(responseBody).optJSONObject("usage");
        } catch (RuntimeException e) {
            LOGGER.debug("No usage block in Anthropic vision response: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject textBlock(String text) {
        return new JSONObject().put("type", "text").put("text", text);
    }

    private JSONObject imageBlock(byte[] imageBytes) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        JSONObject source = new JSONObject()
                .put("type", "base64")
                .put("media_type", "image/png")
                .put("data", base64);
        return new JSONObject().put("type", "image").put("source", source);
    }
}
