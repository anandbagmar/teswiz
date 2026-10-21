package com.znsio.teswiz.visual;

/**
 * OpenRouter provider. OpenRouter is an OpenAI-compatible aggregator, so the request body,
 * {@code Bearer} authorization, and response parsing are inherited unchanged from
 * {@link OpenAiLlmVisionProvider}. Its non-billable {@code /api/v1/models} listing is the sibling
 * of {@code /api/v1/chat/completions}, which the inherited {@code /chat/completions -> /models}
 * derivation already produces.
 */
public class OpenRouterLlmVisionProvider extends OpenAiLlmVisionProvider {
}
