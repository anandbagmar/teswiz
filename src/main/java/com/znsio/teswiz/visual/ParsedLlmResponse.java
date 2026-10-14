package com.znsio.teswiz.visual;

/**
 * Provider-agnostic view of a single LLM vision response: the assistant's textual content (which
 * the caller then parses for a bounding-box JSON object) plus token usage figures normalised across
 * providers.
 */
public record ParsedLlmResponse(String content, long promptTokens, long completionTokens, long totalTokens) {
}
