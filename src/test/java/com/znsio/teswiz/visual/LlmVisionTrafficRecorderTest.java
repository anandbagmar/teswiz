package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LlmVisionTrafficRecorderTest {

    @Test
    void writesRequestAndResponseIntoLlmVisionTrafficFolder(@TempDir Path scenarioDir) throws Exception {
        LlmVisionTrafficRecorder recorder = new LlmVisionTrafficRecorder(() -> scenarioDir);

        String relativePath = recorder.record(
                "POST",
                "https://api.example.com/v1/chat/completions",
                "{Authorization=***REDACTED***, content-type=application/json}",
                "{\"model\":\"gpt-4o\"}",
                200,
                "{content-type=application/json}",
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"found\\\":true}\"}}]}");

        Path trafficDir = scenarioDir.resolve("llm-vision-traffic");
        assertThat(Files.isDirectory(trafficDir)).isTrue();

        Path written = Files.list(trafficDir).findFirst().orElseThrow();
        assertThat(written.getFileName().toString()).endsWith(".log");
        assertThat(relativePath).contains("llm-vision-traffic");

        String content = Files.readString(written);
        assertThat(content)
                .contains("=== LLM VISION REQUEST ===")
                .contains("POST https://api.example.com/v1/chat/completions")
                .contains("=== LLM VISION RESPONSE ===")
                .contains("Status: 200")
                .contains("gpt-4o");
    }

    @Test
    void authorizationHeaderValueIsMaskedAndNeverLeaked(@TempDir Path scenarioDir) throws Exception {
        LlmVisionTrafficRecorder recorder = new LlmVisionTrafficRecorder(() -> scenarioDir);

        // Even if a raw-looking secret reaches the recorder, SensitiveDataMasker masks it before write.
        recorder.record("POST", "https://api.example.com/v1/chat/completions",
                "{Authorization=sk-super-secret-value-123}", "{}", 200, "{}", "{}");

        Path written = Files.list(scenarioDir.resolve("llm-vision-traffic")).findFirst().orElseThrow();
        String content = Files.readString(written);
        // the raw secret must not appear; the Authorization value is masked
        assertThat(content).doesNotContain("sk-super-secret-value-123");
        assertThat(content).contains("Authorization=***");
    }

    @Test
    void eachCallGetsAnIncrementingIndexedFileName(@TempDir Path scenarioDir) throws Exception {
        LlmVisionTrafficRecorder recorder = new LlmVisionTrafficRecorder(() -> scenarioDir);

        recorder.record("POST", "https://api.example.com/a", "{}", "{}", 200, "{}", "{}");
        recorder.record("POST", "https://api.example.com/b", "{}", "{}", 200, "{}", "{}");

        long count = Files.list(scenarioDir.resolve("llm-vision-traffic")).count();
        assertThat(count).isEqualTo(2);
    }
}
