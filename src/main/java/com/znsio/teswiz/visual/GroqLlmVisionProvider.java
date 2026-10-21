package com.znsio.teswiz.visual;

/**
 * Groq provider. Groq exposes an OpenAI-compatible chat-completions API, so the request body,
 * {@code Bearer} authorization, and response parsing are inherited unchanged from
 * {@link OpenAiLlmVisionProvider}. Its non-billable {@code /models} listing is the sibling of
 * {@code /openai/v1/chat/completions}, which the inherited {@code /chat/completions -> /models}
 * derivation already produces.
 */
public class GroqLlmVisionProvider extends OpenAiLlmVisionProvider {
}
