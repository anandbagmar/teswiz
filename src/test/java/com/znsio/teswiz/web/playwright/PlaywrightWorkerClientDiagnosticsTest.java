package com.znsio.teswiz.web.playwright;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.znsio.teswiz.exceptions.InvalidTestDataException;

/**
 * When the Node worker dies (most often because the project has not installed the playwright npm
 * package), the error must carry what the worker printed to stderr, not just "closed unexpectedly".
 */
class PlaywrightWorkerClientDiagnosticsTest {
    private static final String MISSING_PACKAGE_ERROR =
            "Error [ERR_MODULE_NOT_FOUND]: Cannot find package 'playwright' imported from worker.mjs";

    @TempDir
    Path tempDir;

    private PlaywrightWorkerClient workerClient;

    @AfterEach
    void stopWorker() {
        if (null != workerClient) {
            workerClient.close();
        }
    }

    @Test
    void shouldReportTheWorkersStderrWhenItExitsBeforeReplying() throws IOException {
        workerClient = new PlaywrightWorkerClient(failingWorker());
        workerClient.start();

        assertThatThrownBy(workerClient::ping)
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("'ping'")
                .hasMessageContaining(MISSING_PACKAGE_ERROR)
                .hasMessageContaining("npm install");
    }

    private Path failingWorker() throws IOException {
        Path script = tempDir.resolve("failing-worker.mjs");
        Files.writeString(script, """
                process.stderr.write("%s\\n");
                process.exit(1);
                """.formatted(MISSING_PACKAGE_ERROR));
        return script;
    }
}
