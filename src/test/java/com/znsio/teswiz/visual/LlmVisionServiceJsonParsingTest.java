package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LlmVisionService#extractJsonObject(String)} - the robustness of pulling the
 * bounding-box JSON out of raw LLM content that may include prose, markdown fences, or noise.
 */
class LlmVisionServiceJsonParsingTest {

    @Test
    void extractsPlainJsonObject() {
        JSONObject box = LlmVisionService.extractJsonObject(
                "{\"found\":true,\"x\":10,\"y\":20,\"width\":30,\"height\":40}");
        assertThat(box).isNotNull();
        assertThat(box.getBoolean("found")).isTrue();
        assertThat(box.getInt("x")).isEqualTo(10);
        assertThat(box.getInt("height")).isEqualTo(40);
    }

    @Test
    void extractsJsonEvenWhenWrappedInProseOrMarkdownFences() {
        String content = "Sure! Here is the location:\n```json\n{\"found\":true,\"x\":5,\"y\":6,\"width\":7,\"height\":8}\n```\nHope that helps.";
        JSONObject box = LlmVisionService.extractJsonObject(content);
        assertThat(box).isNotNull();
        assertThat(box.getInt("x")).isEqualTo(5);
        assertThat(box.getInt("width")).isEqualTo(7);
    }

    @Test
    void returnsNullWhenNoJsonObjectPresent() {
        assertThat(LlmVisionService.extractJsonObject("I could not find the element.")).isNull();
    }

    @Test
    void returnsNullForMalformedJson() {
        // starts with { and ends with } but is not valid JSON
        assertThat(LlmVisionService.extractJsonObject("{ not : valid, json ]")).isNull();
    }

    @Test
    void returnsNullWhenBracesAreInverted() {
        assertThat(LlmVisionService.extractJsonObject("} x {")).isNull();
    }
}
