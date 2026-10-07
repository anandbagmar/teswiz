package com.znsio.teswiz.analytics;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class TelemetryCollector {
    private static final Logger LOGGER = LogManager.getLogger(TelemetryCollector.class.getName());
    private static final AtomicInteger totalScenarios = new AtomicInteger(0);
    private static final AtomicInteger passedScenarios = new AtomicInteger(0);
    private static final AtomicInteger failedScenarios = new AtomicInteger(0);
    private static long startTimeMs = System.currentTimeMillis();
    private static boolean isStartedEventSent = false;
    private static boolean isCompletedEventSent = false;

    public static synchronized void sendRunStartedEvent() {
        if (!TelemetryConfig.isTelemetryEnabled()) {
            LOGGER.debug("Usage Telemetry is disabled via config.");
            return;
        }
        if (isStartedEventSent) {
            return;
        }
        startTimeMs = System.currentTimeMillis();
        JSONObject startPayload = RunTelemetry.buildStartEventPayload();
        saveLocalTelemetry("run-started.json", startPayload);
        sendRemoteTelemetryAsync(startPayload);
        isStartedEventSent = true;
    }

    public static void recordScenarioOutcome(boolean isSuccess) {
        totalScenarios.incrementAndGet();
        if (isSuccess) {
            passedScenarios.incrementAndGet();
        } else {
            failedScenarios.incrementAndGet();
        }
    }

    public static synchronized void sendRunCompletedEvent() {
        if (!TelemetryConfig.isTelemetryEnabled()) {
            return;
        }
        if (isCompletedEventSent) {
            return;
        }
        long durationMs = System.currentTimeMillis() - startTimeMs;
        JSONObject completedPayload = RunTelemetry.buildCompletedEventPayload(
                totalScenarios.get(),
                passedScenarios.get(),
                failedScenarios.get(),
                durationMs
        );
        saveLocalTelemetry("run-completed.json", completedPayload);
        sendRemoteTelemetryAsync(completedPayload);
        isCompletedEventSent = true;
    }

    private static void saveLocalTelemetry(String fileName, JSONObject payload) {
        try {
            File targetDir = new File("target/teswiz-analytics");
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            File jsonFile = new File(targetDir, fileName);
            FileUtils.writeStringToFile(jsonFile, payload.toString(2), StandardCharsets.UTF_8);
            LOGGER.debug("Saved local telemetry artifact to: " + jsonFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.debug("Failed to write local telemetry artifact: " + e.getMessage());
        }
    }

    private static void sendRemoteTelemetryAsync(JSONObject payload) {
        CompletableFuture.runAsync(() -> {
            try {
                String endpoint = TelemetryConfig.getTelemetryEndpoint();
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(1))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(2))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                LOGGER.debug("Telemetry POST to {} returned status: {}", endpoint, response.statusCode());
            } catch (Exception e) {
                LOGGER.debug("Fail-silent error sending telemetry POST to collector: " + e.getMessage());
            }
        });
    }

    public static void resetForTesting() {
        totalScenarios.set(0);
        passedScenarios.set(0);
        failedScenarios.set(0);
        isStartedEventSent = false;
        isCompletedEventSent = false;
        startTimeMs = System.currentTimeMillis();
    }
}
