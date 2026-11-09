package com.znsio.teswiz.web.playwright;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.znsio.teswiz.exceptions.InvalidTestDataException;

class PlaywrightWorkerScriptResolverTest {
    @TempDir
    Path projectRoot;

    @Test
    void shouldPreferTheProjectsOwnWorkerScript() throws IOException {
        Path projectWorker = Files.createDirectories(projectRoot.resolve("playwright")).resolve("worker.mjs");
        Files.writeString(projectWorker, "// project worker");

        Path resolved = new PlaywrightWorkerScriptResolver().resolve(projectRoot);

        assertThat(resolved).isEqualTo(projectWorker);
    }

    @Test
    void shouldExtractTheBundledWorkerWhenTheProjectHasNone() throws IOException {
        Path resolved = new PlaywrightWorkerScriptResolver().resolve(projectRoot);

        assertThat(resolved).isEqualTo(projectRoot.resolve("build/teswiz/playwright/worker.mjs"));
        assertThat(Files.readString(resolved)).isEqualTo(bundled("worker.mjs"));
        assertThat(Files.readString(resolved.resolveSibling("worker-provider.mjs")))
                .isEqualTo(bundled("worker-provider.mjs"));
    }

    @Test
    void shouldRefreshAPreviouslyExtractedWorkerThatNoLongerMatchesTheBundle() throws IOException {
        Path stale = Files.createDirectories(projectRoot.resolve("build/teswiz/playwright")).resolve("worker.mjs");
        Files.writeString(stale, "// worker from an older teswiz release");

        Path resolved = new PlaywrightWorkerScriptResolver().resolve(projectRoot);

        assertThat(Files.readString(resolved)).isEqualTo(bundled("worker.mjs"));
    }

    @Test
    void shouldExplainHowToFixAMissingWorker() {
        PlaywrightWorkerScriptResolver resolver = new PlaywrightWorkerScriptResolver("no/such/bundle/");

        assertThatThrownBy(() -> resolver.resolve(projectRoot))
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining(projectRoot.resolve("playwright/worker.mjs").toString())
                .hasMessageContaining("teswiz jar");
    }

    private static String bundled(String fileName) throws IOException {
        try (InputStream stream = PlaywrightWorkerScriptResolverTest.class.getClassLoader()
                .getResourceAsStream(PlaywrightWorkerScriptResolver.BUNDLED_WORKER_RESOURCE_DIR + fileName)) {
            assertThat(stream).as("bundled %s on the classpath", fileName).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
