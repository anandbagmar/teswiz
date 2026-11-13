package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for what {@link EmailSender} tells an operator when a delivery fails. The outcome stays {@code FAILED}
 * and nothing is thrown; the log says what failed (type and root cause), which SMTP host and port, with which timeout
 * — and never prints the SMTP password.
 */
class EmailSenderDiagnosticsTest {

    private static final String PASSWORD = "SMTP-SECRET-PASSWORD";
    private static final EmailSender.SmtpSettings SMTP = new EmailSender.SmtpSettings("smtp.corp", "25", "none",
            false, "ci@corp", "", PASSWORD);
    private static final Duration NINE_SECONDS = Duration.ofSeconds(9);
    private static final List<String> RECIPIENTS = List.of("a@x.com");

    private static EmailSender.Transmit failingWith(Throwable cause) {
        return (recipients, subject, html) -> {
            throw new EmailSender.EmailTransmitException("Failed to transmit email via SMTP host smtp.corp", cause);
        };
    }

    @Test
    void aFailedSendLogsTheRootCauseHostPortAndTimeout() {
        EmailSender sender = new EmailSender(failingWith(new SocketTimeoutException("Connect timed out")), SMTP,
                NINE_SECONDS);

        try (LogCapture log = LogCapture.of(EmailSender.class)) {
            NotificationOutcome outcome = sender.send(RECIPIENTS, "subject", "<html></html>");

            assertThat(outcome).isEqualTo(NotificationOutcome.FAILED);
            assertThat(log.allMessages()).contains("SocketTimeoutException: Connect timed out")
                    .contains("host=smtp.corp").contains("port=25").contains("timeout=9s");
        }
    }

    @Test
    void theSmtpPasswordNeverAppearsInTheLog() {
        EmailSender sender = new EmailSender(failingWith(new IllegalStateException("auth failed for " + PASSWORD)),
                SMTP, NINE_SECONDS);

        try (LogCapture log = LogCapture.of(EmailSender.class)) {
            sender.send(RECIPIENTS, "subject", "<html></html>");

            assertThat(log.allMessages()).doesNotContain(PASSWORD).contains("<redacted>");
        }
    }

    @Test
    void aSuccessfulSendIsSent() {
        EmailSender sender = new EmailSender((recipients, subject, html) -> {
        }, SMTP, NINE_SECONDS);

        NotificationOutcome outcome = sender.send(RECIPIENTS, "subject", "<html></html>");

        assertThat(outcome).isEqualTo(NotificationOutcome.SENT);
    }

    @Test
    void aSenderWithOnlyATransmitSeamStillLogsAFailureWithoutAHost() {
        EmailSender sender = new EmailSender(failingWith(new SocketTimeoutException("Connect timed out")));

        try (LogCapture log = LogCapture.of(EmailSender.class)) {
            NotificationOutcome outcome = sender.send(RECIPIENTS, "subject", "<html></html>");

            assertThat(outcome).isEqualTo(NotificationOutcome.FAILED);
            assertThat(log.allMessages()).contains("SocketTimeoutException").contains("host=unknown");
        }
    }

    @Test
    void theSessionUsesTheConfiguredTimeoutForEverySmtpPhase() {
        Properties properties = EmailSender.sessionProperties(SMTP, NINE_SECONDS);

        assertThat(properties.getProperty("mail.smtp.connectiontimeout")).isEqualTo("9000");
        assertThat(properties.getProperty("mail.smtp.timeout")).isEqualTo("9000");
        assertThat(properties.getProperty("mail.smtp.writetimeout")).isEqualTo("9000");
    }

    @Test
    void theDefaultSessionTimeoutStaysFifteenSeconds() {
        Properties properties = EmailSender.sessionProperties(SMTP);

        assertThat(properties.getProperty("mail.smtp.connectiontimeout")).isEqualTo("15000");
    }
}
