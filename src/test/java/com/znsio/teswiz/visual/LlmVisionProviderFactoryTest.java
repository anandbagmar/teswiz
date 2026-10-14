package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LlmVisionProviderFactoryTest {

    private static final String API_VERSION = "2024-06-01";
    private static final String API_KEY = "key-123";
    private static final GcpAccessTokenProvider TOKEN_PROVIDER = () -> "test-token";

    @Test
    void create_mapsOpenaiToOpenAiProvider() {
        assertThat(create("openai")).isInstanceOf(OpenAiLlmVisionProvider.class);
    }

    @Test
    void create_mapsCustomToOpenAiCompatibleProvider() {
        assertThat(create("custom")).isInstanceOf(OpenAiLlmVisionProvider.class);
    }

    @Test
    void create_mapsAzureOpenaiToAzureProvider() {
        assertThat(create("azure-openai")).isInstanceOf(AzureOpenAiLlmVisionProvider.class);
    }

    @Test
    void create_mapsAnthropicToAnthropicProvider() {
        assertThat(create("anthropic")).isInstanceOf(AnthropicLlmVisionProvider.class);
    }

    @Test
    void create_mapsGeminiToGeminiProvider() {
        assertThat(create("gemini")).isInstanceOf(GeminiLlmVisionProvider.class);
    }

    @Test
    void create_mapsVertexGeminiToVertexProvider() {
        assertThat(create("vertex-gemini")).isInstanceOf(VertexGeminiLlmVisionProvider.class);
    }

    @Test
    void create_isCaseInsensitive() {
        assertThat(create("OpenAI")).isInstanceOf(OpenAiLlmVisionProvider.class);
    }

    private LlmVisionProvider create(String provider) {
        return LlmVisionProviderFactory.create(provider, API_VERSION, API_KEY, TOKEN_PROVIDER);
    }
}
