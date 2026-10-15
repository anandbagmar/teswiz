package com.znsio.teswiz.visual;

/**
 * xAI (Grok vision) provider. xAI exposes an OpenAI-compatible chat-completions API, so the request
 * body, {@code Bearer} authorization, and response parsing are inherited unchanged from
 * {@link OpenAiLlmVisionProvider}. Its non-billable {@code /v1/models} listing is the sibling of
 * {@code /v1/chat/completions}, which the inherited {@code /chat/completions -> /models} derivation
 * already produces.
 */
public class XaiLlmVisionProvider extends OpenAiLlmVisionProvider {
}
