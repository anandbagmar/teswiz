package com.znsio.teswiz.reporting.notify;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.znsio.teswiz.network.FailureDetail;
import com.znsio.teswiz.network.FallbackHttpClient;
import com.znsio.teswiz.network.HttpRequest;
import com.znsio.teswiz.network.ProxySettings;

/**
 * Posts a composed Slack message to a per-type incoming-webhook URL. A Slack incoming webhook already targets one
 * channel, so the URL is the whole destination (and the secret) — it is never logged. The sender never throws: a blank
 * webhook short-circuits to {@link NotificationOutcome#SKIPPED_NO_DESTINATION}, and any transport error or non-2xx
 * response is swallowed to {@link NotificationOutcome#FAILED}. A notifier problem must never fail the run.
 * <p>
 * When a send fails, the log says what failed (exception type and root cause), where (the webhook host only), and with
 * which timeout and proxy. The request goes through teswiz's shared {@link FallbackHttpClient}: when the Slack host has
 * several addresses and one cannot be reached, the next one is tried (before anything is sent) and a network note is
 * logged naming the unreachable address.
 * <p>
 * The request timeout defaults to {@value #DEFAULT_TIMEOUT_SECONDS} seconds. The request goes through the proxy teswiz
 * is configured with ({@code PROXY_URL} / {@code PROXY_KEY}), if any.
 * <p>
 * The actual HTTP POST is a {@link HttpPost} seam so unit tests exercise the outcome mapping offline.
 */
public final class SlackSender {

    // LogManager/Logger are fully-qualified ON PURPOSE (same as JUnitResults): an auto-import-organizer in this
    // environment rewrites a plain log4j import to java.util.logging.LogManager and drops Logger, breaking the build.
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
            .getLogger(SlackSender.class.getName());

    private static final long DEFAULT_TIMEOUT_SECONDS = 20;
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);
    private static final int HTTP_OK_LOWER = 200;
    private static final int HTTP_OK_UPPER = 300;
    private static final String UNKNOWN_HOST = "unknown";
    private static final String JSON = "application/json";
    private static final String PURPOSE = "slack";

    private final HttpPost httpPost;
    private final Duration timeout;
    private final ProxySettings proxy;

    /**
     * Creates a sender using the shared fallback HTTP client, the default timeout and the configured proxy.
     */
    public SlackSender() {
        this(DEFAULT_TIMEOUT);
    }

    /**
     * Creates a sender using the shared fallback HTTP client and the configured proxy, with the given timeout.
     *
     * @param timeout the request timeout
     */
    public SlackSender(Duration timeout) {
        this((webhookUrl, jsonBody) -> postViaFallbackClient(webhookUrl, jsonBody, timeout), timeout,
                ProxySettings.fromEnvironment());
    }

    /**
     * Creates a sender with an injected HTTP POST — the unit-test seam. No proxy is used.
     *
     * @param httpPost the POST implementation, returning the HTTP status code
     */
    public SlackSender(HttpPost httpPost) {
        this(httpPost, DEFAULT_TIMEOUT, ProxySettings.none());
    }

    SlackSender(HttpPost httpPost, Duration timeout, ProxySettings proxy) {
        this.httpPost = httpPost;
        this.timeout = timeout;
        this.proxy = proxy;
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
            return isSuccess(status) ? NotificationOutcome.SENT : logFailure(status, webhookUrl);
        } catch (IOException | InterruptedException | RuntimeException e) {
            return logTransportFailure(e, webhookUrl);
        }
    }

    private NotificationOutcome logFailure(int status, String webhookUrl) {
        LOGGER.warn("Slack webhook returned non-success status {}; treated as FAILED (run unaffected). {}", status,
                context(hostOf(webhookUrl)));
        return NotificationOutcome.FAILED;
    }

    private NotificationOutcome logTransportFailure(Exception failure, String webhookUrl) {
        if (failure instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        LOGGER.warn("Slack delivery failed; swallowed so the run is unaffected: {} {}",
                FailureDetail.describe(failure, webhookUrl), context(hostOf(webhookUrl)));
        return NotificationOutcome.FAILED;
    }

    private String context(String host) {
        return "[host=" + host + ", timeout=" + timeout.toSeconds() + "s, proxy=" + proxy.describe() + "]";
    }

    private static String hostOf(String webhookUrl) {
        try {
            String host = URI.create(webhookUrl).getHost();
            return host == null ? UNKNOWN_HOST : host;
        } catch (IllegalArgumentException e) {
            return UNKNOWN_HOST;
        }
    }

    private static boolean isSuccess(int status) {
        return status >= HTTP_OK_LOWER && status < HTTP_OK_UPPER;
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

    private static int postViaFallbackClient(String webhookUrl, String jsonBody, Duration timeout)
            throws IOException {
        HttpRequest request = HttpRequest.post(webhookUrl, JSON, jsonBody.getBytes(StandardCharsets.UTF_8), timeout);
        return new FallbackHttpClient(PURPOSE).send(request).status();
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
