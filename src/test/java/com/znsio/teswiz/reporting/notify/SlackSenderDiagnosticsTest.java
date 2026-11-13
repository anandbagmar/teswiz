package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpConnectTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import com.znsio.teswiz.network.ProxySettings;

/**
 * Unit tests for what {@link SlackSender} tells an operator when a delivery fails, and for how the default sender
 * connects. The outcome stays {@code FAILED} and nothing is thrown; the log says what failed, where, with which timeout
 * and proxy — without ever printing the webhook URL (it is the secret). Per-address connection problems are logged by
 * the shared client as network notes.
 */
class SlackSenderDiagnosticsTest {

    private static final String WEBHOOK = "https://hooks.slack.com/services/T000/B000/SECRETVALUE";
    private static final Duration SEVEN_SECONDS = Duration.ofSeconds(7);

    private static SlackSender.HttpPost connectTimeout() {
        return (url, body) -> {
            throw new HttpConnectTimeoutException("HTTP connect timed out");
        };
    }

    @Test
    void aFailedSendLogsTheExceptionTypeHostTimeoutAndProxy() {
        SlackSender sender = new SlackSender(connectTimeout(), SEVEN_SECONDS, ProxySettings.none());

        try (LogCapture log = LogCapture.of(SlackSender.class)) {
            NotificationOutcome outcome = sender.send(WEBHOOK, "hello");

            assertThat(outcome).isEqualTo(NotificationOutcome.FAILED);
            assertThat(log.allMessages()).contains("HttpConnectTimeoutException: HTTP connect timed out")
                    .contains("host=hooks.slack.com").contains("timeout=7s").contains("proxy=none");
        }
    }

    @Test
    void aFailedSendThroughAProxyNamesTheProxy() {
        Map<String, String> teswizConfig = Map.of("PROXY_URL", "http://proxy.corp:3128");
        ProxySettings proxy = ProxySettings.resolve(teswizConfig::get, Map.<String, String>of()::get);
        SlackSender sender = new SlackSender(connectTimeout(), SEVEN_SECONDS, proxy);

        try (LogCapture log = LogCapture.of(SlackSender.class)) {
            sender.send(WEBHOOK, "hello");

            assertThat(log.allMessages()).contains("proxy=proxy.corp:3128");
        }
    }

    @Test
    void theWebhookUrlNeverAppearsInTheLogEvenWhenTheExceptionEchoesIt() {
        SlackSender.HttpPost echoingTheUrl = (url, body) -> {
            throw new IllegalArgumentException("Illegal character in " + url);
        };
        SlackSender sender = new SlackSender(echoingTheUrl, SEVEN_SECONDS, ProxySettings.none());

        try (LogCapture log = LogCapture.of(SlackSender.class)) {
            sender.send(WEBHOOK, "hello");

            assertThat(log.allMessages()).doesNotContain("SECRETVALUE").doesNotContain("/services/")
                    .contains("<redacted>");
        }
    }

    @Test
    void aNonSuccessStatusLogsTheStatusAndHost() {
        SlackSender sender = new SlackSender((url, body) -> 403, SEVEN_SECONDS, ProxySettings.none());

        try (LogCapture log = LogCapture.of(SlackSender.class)) {
            NotificationOutcome outcome = sender.send(WEBHOOK, "hello");

            assertThat(outcome).isEqualTo(NotificationOutcome.FAILED);
            assertThat(log.allMessages()).contains("403").contains("host=hooks.slack.com");
        }
    }

    @Test
    void theDefaultTimeoutIsTwentySeconds() {
        SlackSender sender = new SlackSender(connectTimeout());

        try (LogCapture log = LogCapture.of(SlackSender.class)) {
            sender.send(WEBHOOK, "hello");

            assertThat(log.allMessages()).contains("timeout=20s");
        }
    }

    @Test
    void aMalformedWebhookStillFailsWithoutThrowingAndNamesNoHost() {
        SlackSender sender = new SlackSender(connectTimeout(), SEVEN_SECONDS, ProxySettings.none());

        try (LogCapture log = LogCapture.of(SlackSender.class)) {
            NotificationOutcome outcome = sender.send("not a url SECRETVALUE", "hello");

            assertThat(outcome).isEqualTo(NotificationOutcome.FAILED);
            assertThat(log.allMessages()).contains("host=unknown").doesNotContain("SECRETVALUE");
        }
    }

    @Test
    void theDefaultSender_postsTheMessageAsJson_throughTheSharedFallbackClient() throws IOException {
        AtomicReference<String> received = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/hook", exchange -> {
            received.set(exchange.getRequestHeaders().getFirst("Content-Type") + " "
                    + new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(200, 2);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write("ok".getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();
        try {
            String webhook = "http://127.0.0.1:" + server.getAddress().getPort() + "/hook";

            NotificationOutcome outcome = new SlackSender(SEVEN_SECONDS).send(webhook, "hello");

            assertThat(outcome).isEqualTo(NotificationOutcome.SENT);
            assertThat(received.get()).startsWith("application/json").endsWith("{\"text\":\"hello\"}");
        } finally {
            server.stop(0);
        }
    }
}
