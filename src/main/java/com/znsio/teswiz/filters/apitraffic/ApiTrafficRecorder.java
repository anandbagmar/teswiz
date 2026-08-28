package com.znsio.teswiz.filters.apitraffic;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.tools.SensitiveDataMasker;

/**
 * Writes one masked log file per API call into the {@code api-traffic}
 * sub-folder of the current scenario's directory (resolved via
 * {@link ScenarioDirectoryResolver}), i.e.
 * {@code reports/<scenario>/api-traffic/NN-METHOD-endpoint.log}. Returns the
 * file path (relative to {@code LOG_DIR} when possible) for the console
 * reference line.
 * <p>
 * Values are masked with teswiz's {@link SensitiveDataMasker} before being
 * written, honouring {@code SHOW_SENSITIVE_DATA} and {@code MASK_ADDITIONAL_KEYS}.
 */
public class ApiTrafficRecorder {

    private static final Logger LOGGER = LogManager.getLogger(ApiTrafficRecorder.class.getName());
    private static final String API_TRAFFIC_DIR = "api-traffic";
    private static final String LOG_DIR_PROPERTY = "LOG_DIR";

    private final ScenarioDirectoryResolver scenarioDirectoryResolver;

    public ApiTrafficRecorder(ScenarioDirectoryResolver scenarioDirectoryResolver) {
        this.scenarioDirectoryResolver = scenarioDirectoryResolver;
    }

    /**
     * Writes the masked request/response detail for a single call to its own file.
     *
     * @return the file path for use in the console reference line (relative to
     *         {@code LOG_DIR} when the file sits under it, otherwise absolute)
     */
    public String record(ApiTrafficRecord record) {
        int index = ApiCallContext.nextIndex();
        Path apiTrafficDir = scenarioDirectoryResolver.resolve().resolve(API_TRAFFIC_DIR);
        String fileName = ApiCallFileNamer.fileName(index, record.method(), record.endpoint());
        Path file = apiTrafficDir.resolve(fileName);

        writeFile(file, buildContent(record));

        return relativeToLogDir(file);
    }

    // --- Private helper methods ---

    private String buildContent(ApiTrafficRecord record) {
        String content = "=== REQUEST ===\n"
                + record.method() + " " + record.endpoint() + "\n\n"
                + "-- Request Headers --\n" + record.requestHeaders() + "\n\n"
                + "-- Request Body --\n" + record.requestBody() + "\n\n"
                + "=== RESPONSE ===\n"
                + "Status: " + record.statusCode() + "\n\n"
                + "-- Response Headers --\n" + record.responseHeaders() + "\n\n"
                + "-- Response Body --\n" + record.responseBody() + "\n";
        return SensitiveDataMasker.mask(content);
    }

    private void writeFile(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (IOException e) {
            LOGGER.warn("Failed to write api-traffic file {}: {}", file, e.getMessage());
            throw new UncheckedIOException("Unable to write api-traffic log file: " + file, e);
        }
    }

    private String relativeToLogDir(Path file) {
        String logDir = System.getProperty(LOG_DIR_PROPERTY);
        if (logDir == null || logDir.isBlank()) {
            return file.toString();
        }
        Path logDirPath = Path.of(logDir).toAbsolutePath().normalize();
        Path absoluteFile = file.toAbsolutePath().normalize();
        return absoluteFile.startsWith(logDirPath)
                ? logDirPath.relativize(absoluteFile).toString()
                : absoluteFile.toString();
    }
}
