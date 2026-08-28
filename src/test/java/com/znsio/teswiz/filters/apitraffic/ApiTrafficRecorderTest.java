package com.znsio.teswiz.filters.apitraffic;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.znsio.teswiz.tools.SensitiveDataMasker;

/**
 * Unit tests for {@link ApiTrafficRecorder} — writes one masked file per API call
 * into the {@code api-traffic} sub-folder of the scenario directory supplied by
 * the resolver, and returns the relative path for the console reference line.
 */
class ApiTrafficRecorderTest {

    @AfterEach
    void tearDown() {
        ApiCallContext.clear();
        SensitiveDataMasker.resetSensitiveKeysToDefault();
    }

    @Test
    void record_writesPerCallFileUnderResolvedApiTrafficFolder(@TempDir Path scenarioDir) {
        ApiTrafficRecorder recorder = recorderFor(scenarioDir);

        recorder.record(sampleRecord());

        Path apiTraffic = scenarioDir.resolve("api-traffic");
        assertThat(Files.isDirectory(apiTraffic)).isTrue();
        assertThat(apiTraffic.toFile().listFiles()).hasSize(1);
    }

    @Test
    void record_fileNameStartsWithZeroPaddedIndexMethod(@TempDir Path scenarioDir) {
        ApiTrafficRecorder recorder = recorderFor(scenarioDir);

        recorder.record(sampleRecord());

        String fileName = scenarioDir.resolve("api-traffic").toFile().listFiles()[0].getName();
        assertThat(fileName).startsWith("01-POST-");
    }

    @Test
    void record_fileContainsRequestAndResponseDetail(@TempDir Path scenarioDir) throws Exception {
        ApiTrafficRecorder recorder = recorderFor(scenarioDir);

        recorder.record(sampleRecord());

        String content = Files.readString(firstFileIn(scenarioDir));
        assertThat(content).contains("POST");
        assertThat(content).contains("/api/v1/balance");
        assertThat(content).contains("200");
        assertThat(content).contains("{\"balance\":100}");
    }

    @Test
    void record_masksTeswizBuiltInSensitiveValues(@TempDir Path scenarioDir) throws Exception {
        ApiTrafficRecorder recorder = recorderFor(scenarioDir);

        ApiTrafficRecord record = new ApiTrafficRecord(
                "POST", "/authenticate?token=secret-token-xyz&game_id=42",
                "Content-Type: application/json", "{\"password\":\"p@ss\"}",
                200, "resp-headers", "{\"ok\":true}");
        recorder.record(record);

        String content = Files.readString(firstFileIn(scenarioDir));
        assertThat(content).doesNotContain("secret-token-xyz");
        assertThat(content).doesNotContain("p@ss");
        assertThat(content).contains("game_id=42");
    }

    @Test
    void record_returnsApiTrafficRelativePathForConsoleReference(@TempDir Path scenarioDir) {
        ApiTrafficRecorder recorder = recorderFor(scenarioDir);

        String relativePath = recorder.record(sampleRecord());

        assertThat(relativePath).contains("api-traffic");
        assertThat(relativePath).endsWith(".log");
    }

    private ApiTrafficRecorder recorderFor(Path scenarioDir) {
        return new ApiTrafficRecorder(() -> scenarioDir);
    }

    private Path firstFileIn(Path scenarioDir) {
        return scenarioDir.resolve("api-traffic").toFile().listFiles()[0].toPath();
    }

    private ApiTrafficRecord sampleRecord() {
        return new ApiTrafficRecord(
                "POST", "/api/v1/balance",
                "Content-Type: application/json", "{\"amount\":10.0}",
                200, "Content-Type: application/json", "{\"balance\":100}");
    }
}
