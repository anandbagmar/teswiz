package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link SlackSender}.
 * <p>
 * The sender POSTs a Slack message payload to a per-type incoming-webhook URL and never lets a delivery problem escape:
 * a missing webhook short-circuits to {@link NotificationOutcome#SKIPPED_NO_DESTINATION}, a transport exception is
 * swallowed to {@link NotificationOutcome#FAILED}, and a 2xx response yields {@link NotificationOutcome#SENT}. The HTTP
 * POST is injected as a seam so these tests run offline and assert the outcome without touching the network.
 */
class SlackSenderTest {

    @Test
    void missingWebhookShortCircuitsToNoDestination() {
        SlackSender sender = new SlackSender((url, body) -> 200);

        NotificationOutcome outcome = sender.send("", "hello");

        assertThat(outcome).isEqualTo(NotificationOutcome.SKIPPED_NO_DESTINATION);
    }

    @Test
    void successfulPostYieldsSent() {
        SlackSender sender = new SlackSender((url, body) -> 200);

        NotificationOutcome outcome = sender.send("https://hooks.slack.com/services/XXX", "hello");

        assertThat(outcome).isEqualTo(NotificationOutcome.SENT);
    }

    @Test
    void nonSuccessResponseYieldsFailedWithoutThrowing() {
        SlackSender sender = new SlackSender((url, body) -> 500);

        NotificationOutcome outcome = sender.send("https://hooks.slack.com/services/XXX", "hello");

        assertThat(outcome).isEqualTo(NotificationOutcome.FAILED);
    }

    @Test
    void transportExceptionIsSwallowedToFailed() {
        SlackSender sender = new SlackSender((url, body) -> {
            throw new IOException("connection refused");
        });

        NotificationOutcome outcome = sender.send("https://hooks.slack.com/services/XXX", "hello");

        assertThat(outcome).isEqualTo(NotificationOutcome.FAILED);
    }
}
