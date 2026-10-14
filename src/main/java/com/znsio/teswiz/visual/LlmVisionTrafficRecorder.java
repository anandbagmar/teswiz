package com.znsio.teswiz.visual;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.filters.apitraffic.ApiCallFileNamer;
import com.znsio.teswiz.filters.apitraffic.ScenarioDirectoryResolver;
import com.znsio.teswiz.filters.apitraffic.TeswizScenarioDirectoryResolver;
import com.znsio.teswiz.tools.SensitiveDataMasker;

/**
 * Writes one masked request/response file per LLM vision call into a dedicated
 * {@code llm-vision-traffic} sub-folder of the current scenario's report directory - kept separate
 * from {@code api-traffic} so LLM calls are not confused with the test's own API calls.
 *
 * <p>Mirrors {@code ApiTrafficRecorder} but with its own per-thread call counter and folder name.
 * Values are masked with {@link SensitiveDataMasker}; callers must additionally redact the
 * {@code Authorization} header before handing content here (defence in depth for the API key).
 */
public final class LlmVisionTrafficRecorder {

    private static final Logger LOGGER = LogManager.getLogger(LlmVisionTrafficRecorder.class.getName());
    private static final String LLM_TRAFFIC_DIR = "llm-vision-traffic";
    private static final String LOG_DIR_PROPERTY = "LOG_DIR";
    private static final ThreadLocal<AtomicInteger> COUNTER = ThreadLocal.withInitial(() -> new AtomicInteger(0));

    private final ScenarioDirectoryResolver scenarioDirectoryResolver;

    public LlmVisionTrafficRecorder() {
        this(new TeswizScenarioDirectoryResolver());
    }

    public LlmVisionTrafficRecorder(ScenarioDirectoryResolver scenarioDirectoryResolver) {
        this.scenarioDirectoryResolver = scenarioDirectoryResolver;
    }

    /**
     * Writes the masked request/response detail for a single LLM vision call to its own file and
     * returns the path (relative to {@code LOG_DIR} when possible) for a console reference line.
     */
    public String record(String method, String endpoint, String requestHeaders, String requestBody,
                          int statusCode, String responseHeaders, String responseBody) {
        int index = COUNTER.get().incrementAndGet();
        Path dir = scenarioDirectoryResolver.resolve().resolve(LLM_TRAFFIC_DIR);
        String fileName = ApiCallFileNamer.fileName(index, method, endpoint);
        Path file = dir.resolve(fileName);

        String content = buildContent(method, endpoint, requestHeaders, requestBody, statusCode, responseHeaders, responseBody);
        writeFile(file, SensitiveDataMasker.mask(content));
        return relativeToLogDir(file);
    }

    private String buildContent(String method, String endpoint, String requestHeaders, String requestBody,
                                int statusCode, String responseHeaders, String responseBody) {
        return "=== LLM VISION REQUEST ===\n"
                + method + " " + endpoint + "\n\n"
                + "-- Request Headers --\n" + requestHeaders + "\n\n"
                + "-- Request Body --\n" + requestBody + "\n\n"
                + "=== LLM VISION RESPONSE ===\n"
                + "Status: " + statusCode + "\n\n"
                + "-- Response Headers --\n" + responseHeaders + "\n\n"
                + "-- Response Body --\n" + responseBody + "\n";
    }

    private void writeFile(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content);
        } catch (IOException e) {
            LOGGER.warn("Failed to write llm-vision-traffic file {}: {}", file, e.getMessage());
            throw new UncheckedIOException("Unable to write llm-vision-traffic log file: " + file, e);
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
