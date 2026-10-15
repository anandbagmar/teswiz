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
 * AWS Bedrock Runtime provider using the model-agnostic {@code Converse} API. Authorization uses a
 * Bedrock API key as a {@code Bearer} token (see
 * <a href="https://docs.aws.amazon.com/bedrock/latest/userguide/api-keys.html">Bedrock API keys</a>),
 * so no AWS SigV4 request signing or AWS SDK is required and the call rides the framework's existing
 * HTTP {@code API_ENGINE} path. The configured endpoint is the full Converse URL
 * ({@code https://bedrock-runtime.<region>.amazonaws.com/model/<modelId>/converse}).
 *
 * <p>The request body is the Converse schema: a top-level {@code system} prompt array, a single
 * {@code user} message whose {@code content} is the user text followed by image blocks (screenshot
 * then any labelled reference images), and an {@code inferenceConfig}. The response's
 * {@code output.message.content[].text} entries are concatenated and {@code usage} supplies token
 * counts.
 *
 * <p>Health-check is structural-only: the Bedrock Runtime endpoint exposes no non-billable
 * models-listing probe under a Bedrock API key, so {@link #buildHealthCheckProbe(String, String)}
 * inherits the default {@link java.util.Optional#empty()}.
 */
public class BedrockLlmVisionProvider implements LlmVisionProvider {

    private static final Logger LOGGER = LogManager.getLogger(BedrockLlmVisionProvider.class.getName());
    private static final int MAX_TOKENS = 1024;

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
        JSONArray system = new JSONArray().put(new JSONObject().put("text", systemPrompt));
        JSONObject inferenceConfig = new JSONObject().put("maxTokens", MAX_TOKENS).put("temperature", 0);

        return new JSONObject()
                .put("system", system)
                .put("messages", messages)
                .put("inferenceConfig", inferenceConfig)
                .toString();
    }

    @Override
    public ParsedLlmResponse parseResponse(String responseBody) {
        String content = extractContent(responseBody);
        JSONObject usage = optUsage(responseBody);
        long promptTokens = usage == null ? 0 : usage.optLong("inputTokens", 0);
        long completionTokens = usage == null ? 0 : usage.optLong("outputTokens", 0);
        long totalTokens = usage == null ? 0 : usage.optLong("totalTokens", promptTokens + completionTokens);
        return new ParsedLlmResponse(content, promptTokens, completionTokens, totalTokens);
    }

    private String extractContent(String responseBody) {
        try {
            JSONObject output = new JSONObject(responseBody).optJSONObject("output");
            if (output == null) {
                return null;
            }
            JSONObject message = output.optJSONObject("message");
            if (message == null) {
                return null;
            }
            JSONArray content = message.optJSONArray("content");
            if (content == null) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                text.append(content.getJSONObject(i).optString("text", ""));
            }
            return text.toString();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not parse Bedrock Converse response envelope: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject optUsage(String responseBody) {
        try {
            return new JSONObject(responseBody).optJSONObject("usage");
        } catch (RuntimeException e) {
            LOGGER.debug("No usage block in Bedrock Converse response: {}", e.getMessage());
            return null;
        }
    }

    private JSONObject textBlock(String text) {
        return new JSONObject().put("text", text);
    }

    private JSONObject imageBlock(byte[] imageBytes) {
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        JSONObject source = new JSONObject().put("bytes", base64);
        JSONObject image = new JSONObject().put("format", "png").put("source", source);
        return new JSONObject().put("image", image);
    }
}
