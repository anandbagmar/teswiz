package com.znsio.teswiz.reporting.notify;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Posts a composed Slack message to a per-type incoming-webhook URL. A Slack incoming webhook already targets one
 * channel, so the URL is the whole destination (and the secret) — it is never logged. The sender never throws: a blank
 * webhook short-circuits to {@link NotificationOutcome#SKIPPED_NO_DESTINATION}, and any transport error or non-2xx
 * response is swallowed to {@link NotificationOutcome#FAILED}. A notifier problem must never fail the run.
 * <p>
 * The actual HTTP POST is a {@link HttpPost} seam so unit tests exercise the outcome mapping offline; the default
 * implementation uses the JDK {@link HttpClient}, the same transport the Sentinel reporter uses.
 */
public final class SlackSender {

    private static final Logger LOGGER = LogManager.getLogger(SlackSender.class.getName());

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int HTTP_OK_LOWER = 200;
    private static final int HTTP_OK_UPPER = 300;

    private final HttpPost httpPost;

    /**
     * Creates a sender using the default JDK HTTP transport.
     */
    public SlackSender() {
        this(SlackSender::postViaJdkHttpClient);
    }

    /**
     * Creates a sender with an injected HTTP POST — the unit-test seam.
     *
     * @param httpPost the POST implementation, returning the HTTP status code
     */
    public SlackSender(HttpPost httpPost) {
        this.httpPost = httpPost;
    }

    /**
     * Posts the message to the webhook, mapping the result to an outcome and never throwing.
     *
     * @param webhookUrl the per-type incoming-webhook URL (the destination + secret)
     * @param message    the composed Slack message text
     * @return the delivery outcome
     */
    public NotificationOutcome send(String webhookUrl, String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            LOGGER.warn("Slack webhook not configured for an enabled type; skipping (no destination).");
            return NotificationOutcome.SKIPPED_NO_DESTINATION;
        }
        try {
            int status = httpPost.post(webhookUrl, toJsonPayload(message));
            return isSuccess(status) ? NotificationOutcome.SENT : logFailure(status);
        } catch (IOException | InterruptedException | RuntimeException e) {
            LOGGER.warn("Slack delivery failed; swallowed so the run is unaffected: {}", e.getMessage());
            return NotificationOutcome.FAILED;
        }
    }

    private static boolean isSuccess(int status) {
        return status >= HTTP_OK_LOWER && status < HTTP_OK_UPPER;
    }

    private static NotificationOutcome logFailure(int status) {
        LOGGER.warn("Slack webhook returned non-success status {}; treated as FAILED (run unaffected).", status);
        return NotificationOutcome.FAILED;
    }

    private static String toJsonPayload(String message) {
        return "{\"text\":" + jsonString(message) + "}";
    }

    private static String jsonString(String raw) {
        StringBuilder json = new StringBuilder("\"");
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> appendChar(json, c);
            }
        }
        return json.append('"').toString();
    }

    private static StringBuilder appendChar(StringBuilder json, char c) {
        if (c < 0x20) {
            return json.append(String.format("\\u%04x", (int) c));
        }
        return json.append(c);
    }

    private static int postViaJdkHttpClient(String webhookUrl, String jsonBody)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(webhookUrl)).timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    /**
     * The HTTP POST seam: posts {@code jsonBody} to {@code webhookUrl} and returns the HTTP status code. Injected so
     * unit tests can assert the outcome mapping without a real network call.
     */
    @FunctionalInterface
    public interface HttpPost {

        /**
         * @param webhookUrl the destination URL
         * @param jsonBody   the request body
         * @return the HTTP status code
         * @throws IOException          on a transport error
         * @throws InterruptedException when the send is interrupted
         */
        int post(String webhookUrl, String jsonBody) throws IOException, InterruptedException;
    }
}
