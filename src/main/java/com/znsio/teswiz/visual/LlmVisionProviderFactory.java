package com.znsio.teswiz.visual;

/**
 * Selects the concrete {@link LlmVisionProvider} for a configured provider name. {@code custom},
 * {@code groq}, {@code xai}, {@code openrouter}, and {@code ollama} all speak the OpenAI-compatible
 * wire format; {@code bedrock} uses the AWS Bedrock {@code Converse} schema. The provider name match
 * is case-insensitive.
 */
public final class LlmVisionProviderFactory {

    private LlmVisionProviderFactory() {
    }

    public static LlmVisionProvider create(String provider, String apiVersion, String apiKey,
                                           GcpAccessTokenProvider accessTokenProvider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase();
        return switch (normalized) {
            case "azure-openai" -> new AzureOpenAiLlmVisionProvider(apiVersion);
            case "anthropic" -> new AnthropicLlmVisionProvider();
            case "gemini" -> new GeminiLlmVisionProvider(apiKey);
            case "vertex-gemini" -> new VertexGeminiLlmVisionProvider(accessTokenProvider);
            case "groq" -> new GroqLlmVisionProvider();
            case "xai" -> new XaiLlmVisionProvider();
            case "openrouter" -> new OpenRouterLlmVisionProvider();
            case "ollama" -> new OllamaLlmVisionProvider();
            case "bedrock" -> new BedrockLlmVisionProvider();
            case "custom", "openai" -> new OpenAiLlmVisionProvider();
            default -> new OpenAiLlmVisionProvider();
        };
    }
}
