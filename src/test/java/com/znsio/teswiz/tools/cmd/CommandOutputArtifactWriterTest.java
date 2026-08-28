package com.znsio.teswiz.tools.cmd;

import com.znsio.teswiz.tools.LoggingContext;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CommandOutputArtifactWriterTest {
    @AfterEach
    void clearContext() {
        LoggingContext.clear();
    }

    @Test
    void shouldWriteMaskedCommandOutputUnderScenarioDirectory(@TempDir Path tempDir) throws Exception {
        LoggingContext.begin("Checkout", 3, 2, tempDir.toString());

        Path artifact = CommandOutputArtifactWriter.write(
                "git rev-parse --abbrev-ref HEAD",
                "{\"token\":\"secret-value\",\"result\":\"ok\"}",
                "password=hidden-value");

        assertThat(artifact).exists().isRegularFile();
        assertThat(artifact.getParent().getFileName().toString()).isEqualTo("commandOutput");
        String content = Files.readString(artifact);
        assertThat(content).contains("COMMAND", "git rev-parse --abbrev-ref HEAD", "STDOUT", "STDERR", "result", "***");
        assertThat(content).doesNotContain("secret-value", "hidden-value");
        assertThat(ThreadContext.get("scenario")).isEqualTo("Checkout");
    }

    @Test
    void shouldMaskSensitiveValuesInTheCommandItself(@TempDir Path tempDir) throws Exception {
        LoggingContext.begin("Login", 1, 1, tempDir.toString());

        Path artifact = CommandOutputArtifactWriter.write(
                "curl -H 'Authorization: Bearer secret-token-xyz' https://example.com",
                "ok", "");

        String content = Files.readString(artifact);
        assertThat(content).contains("COMMAND");
        assertThat(content).doesNotContain("secret-token-xyz");
    }
}
