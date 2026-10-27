package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link EmailSender}.
 * <p>
 * The sender builds and transmits an HTML email to a resolved recipient list and never lets a delivery problem escape:
 * an empty recipient list short-circuits to {@link NotificationOutcome#SKIPPED_NO_DESTINATION}, a transmit exception is
 * swallowed to {@link NotificationOutcome#FAILED}, and a clean transmit yields {@link NotificationOutcome#SENT}. The
 * transmit step is injected so these tests assert the outcome offline, without a real SMTP server. The live
 * {@code jakarta.mail} transmission path is exercised only in a real run (unverified pending SMTP relay confirmation,
 * GAMC-19572 open question #1).
 */
class EmailSenderTest {

    @Test
    void emptyRecipientsShortCircuitsToNoDestination() {
        EmailSender sender = new EmailSender((recipients, subject, html) -> {
            // not reached
        });

        NotificationOutcome outcome = sender.send(List.of(), "subject", "<html></html>");

        assertThat(outcome).isEqualTo(NotificationOutcome.SKIPPED_NO_DESTINATION);
    }

    @Test
    void cleanTransmitYieldsSent() {
        EmailSender sender = new EmailSender((recipients, subject, html) -> {
            // simulate a successful transmit
        });

        NotificationOutcome outcome = sender.send(List.of("a@x.com"), "subject", "<html></html>");

        assertThat(outcome).isEqualTo(NotificationOutcome.SENT);
    }

    @Test
    void transmitExceptionIsSwallowedToFailed() {
        EmailSender sender = new EmailSender((recipients, subject, html) -> {
            throw new EmailSender.EmailTransmitException("smtp unreachable", null);
        });

        NotificationOutcome outcome = sender.send(List.of("a@x.com"), "subject", "<html></html>");

        assertThat(outcome).isEqualTo(NotificationOutcome.FAILED);
    }
}
