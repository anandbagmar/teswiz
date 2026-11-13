package com.znsio.teswiz.reporting.notify;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import com.znsio.teswiz.network.FailureDetail;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Sends an HTML email to a resolved recipient list over SMTP using {@code jakarta.mail}. Like the Slack sender, it
 * never throws from a delivery problem: an empty recipient list short-circuits to
 * {@link NotificationOutcome#SKIPPED_NO_DESTINATION}, and any transmit error is swallowed to
 * {@link NotificationOutcome#FAILED}. A notifier problem must never fail the run.
 * <p>
 * When a send fails, the log says what failed (exception type and root cause), which SMTP host and port, and with which
 * timeout — so a relay that cannot be reached from the machine running the tests can be told apart from a relay that
 * rejected the message. The SMTP password is never logged.
 * <p>
 * Every SMTP phase (connect, read, write) is bounded by one timeout, {@value #DEFAULT_TIMEOUT_SECONDS} seconds by
 * default, so a stalled relay fails fast instead of hanging the notify task. SMTP does not use the per-address fallback
 * of {@code com.znsio.teswiz.network.FallbackHttpClient}: {@code jakarta.mail} opens the socket itself.
 * <p>
 * The email body is the self-contained HTML the composers build; the cucumber report is linked (on CI), never attached
 * — corporate mail gateways routinely strip/quarantine {@code .zip} attachments, so attaching the report made the whole
 * message silently undeliverable (GAMC-19572).
 * <p>
 * The actual SMTP transmission is a {@link Transmit} seam so unit tests assert the outcome offline without a real mail
 * server. The default transmit uses {@code jakarta.mail}; it sends with credentials when {@code auth} is on and without
 * them against an unauthenticated relay (e.g. an internal MTA on port 25).
 * <p>
 * NOTE on imports: {@code jakarta.mail.Session} / {@code Message} / {@code Transport} are referenced by their fully
 * qualified names rather than imported, because {@code javax.websocket.Session} is also on the test classpath (a teswiz
 * transitive) and an auto-organize-imports would otherwise resolve a short {@code Session} to the wrong type. The
 * log4j {@code Logger} / {@code LogManager} are fully qualified for the same reason (see {@code JUnitResults}).
 */
public final class EmailSender {

    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
            .getLogger(EmailSender.class.getName());

    private static final String HTML_UTF8 = "text/html; charset=utf-8";
    private static final long DEFAULT_TIMEOUT_SECONDS = 15;
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS);
    private static final int DEFAULT_SMTP_PORT = 25;
    private static final String UNKNOWN_HOST = "unknown";

    private final Transmit transmit;
    private final SmtpSettings smtp;
    private final Duration timeout;

    /**
     * Creates a sender that transmits via {@code jakarta.mail} using the given SMTP settings and the default timeout.
     *
     * @param smtp the SMTP connection facts and the password
     */
    public EmailSender(SmtpSettings smtp) {
        this(smtp, DEFAULT_TIMEOUT);
    }

    /**
     * Creates a sender that transmits via {@code jakarta.mail} using the given SMTP settings and timeout.
     *
     * @param smtp    the SMTP connection facts and the password
     * @param timeout the timeout for every SMTP phase (connect, read, write)
     */
    public EmailSender(SmtpSettings smtp, Duration timeout) {
        this(defaultTransmit(smtp, timeout), smtp, timeout);
    }

    /**
     * Creates a sender with an injected transmit step — the unit-test seam. No SMTP host is known, so a failure is
     * logged without one.
     *
     * @param transmit the transmit implementation
     */
    public EmailSender(Transmit transmit) {
        this(transmit, null, DEFAULT_TIMEOUT);
    }

    EmailSender(Transmit transmit, SmtpSettings smtp, Duration timeout) {
        this.transmit = transmit;
        this.smtp = smtp;
        this.timeout = timeout;
    }

    /**
     * Transmits the HTML email, mapping the result to an outcome and never throwing.
     *
     * @param recipients the resolved recipient addresses
     * @param subject    the email subject
     * @param htmlBody   the self-contained HTML body
     * @return the delivery outcome
     */
    public NotificationOutcome send(List<String> recipients, String subject, String htmlBody) {
        if (recipients == null || recipients.isEmpty()) {
            LOGGER.warn("Email enabled but no recipients resolved; skipping (no destination).");
            return NotificationOutcome.SKIPPED_NO_DESTINATION;
        }
        try {
            transmit.transmit(recipients, subject, htmlBody);
            LOGGER.info("Email sent to {} recipient(s).", recipients.size());
            return NotificationOutcome.SENT;
        } catch (EmailTransmitException e) {
            return logFailure(e);
        }
    }

    /**
     * Maps the SMTP settings to a {@code jakarta.mail} session {@link Properties} with the default timeout. Pure and
     * side-effect-free; exposed so the config-to-session mapping can be asserted directly.
     *
     * @param smtp the SMTP connection facts
     * @return the jakarta.mail session properties
     */
    public static Properties sessionProperties(SmtpSettings smtp) {
        return sessionProperties(smtp, DEFAULT_TIMEOUT);
    }

    /**
     * Maps the SMTP settings to a {@code jakarta.mail} session {@link Properties} — host, port, whether AUTH is
     * required, whether STARTTLS is enabled (only when {@code tls} is {@code starttls}; a plaintext port-25 relay
     * leaves it off), and the timeout for every SMTP phase. Pure and side-effect-free.
     *
     * @param smtp    the SMTP connection facts
     * @param timeout the timeout for every SMTP phase (connect, read, write)
     * @return the jakarta.mail session properties
     */
    public static Properties sessionProperties(SmtpSettings smtp, Duration timeout) {
        String timeoutMillis = String.valueOf(timeout.toMillis());
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtp.host());
        properties.put("mail.smtp.port", smtp.port());
        properties.put("mail.smtp.auth", Boolean.toString(smtp.auth()));
        properties.put("mail.smtp.starttls.enable", Boolean.toString("starttls".equalsIgnoreCase(smtp.tls())));
        properties.put("mail.smtp.connectiontimeout", timeoutMillis);
        properties.put("mail.smtp.timeout", timeoutMillis);
        properties.put("mail.smtp.writetimeout", timeoutMillis);
        return properties;
    }

    private NotificationOutcome logFailure(EmailTransmitException failure) {
        LOGGER.warn("Email delivery failed; swallowed so the run is unaffected: {} [host={}, port={}, timeout={}s]",
                FailureDetail.describe(failure, password()), host(), port(), timeout.toSeconds());
        return NotificationOutcome.FAILED;
    }

    private String host() {
        return smtp == null || smtp.host() == null || smtp.host().isBlank() ? UNKNOWN_HOST : smtp.host();
    }

    private int port() {
        try {
            return smtp == null ? DEFAULT_SMTP_PORT : Integer.parseInt(smtp.port().trim());
        } catch (RuntimeException e) {
            return DEFAULT_SMTP_PORT;
        }
    }

    private String password() {
        return smtp == null ? "" : smtp.password();
    }

    private static Transmit defaultTransmit(SmtpSettings smtp, Duration timeout) {
        return (recipients, subject, htmlBody) -> transmitViaJakarta(smtp, timeout, recipients, subject, htmlBody);
    }

    private static void transmitViaJakarta(SmtpSettings smtp, Duration timeout, List<String> recipients,
            String subject, String htmlBody) throws EmailTransmitException {
        try {
            jakarta.mail.Session session = jakarta.mail.Session.getInstance(sessionProperties(smtp, timeout));
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtp.from()));
            message.setRecipients(jakarta.mail.Message.RecipientType.TO,
                    InternetAddress.parse(String.join(",", recipients)));
            message.setSubject(subject);
            message.setContent(htmlBody, HTML_UTF8);
            sendMessage(smtp, message);
        } catch (Exception e) {
            throw new EmailTransmitException("Failed to transmit email via SMTP host " + smtp.host(), e);
        }
    }

    private static void sendMessage(SmtpSettings smtp, MimeMessage message) throws Exception {
        if (smtp.auth()) {
            jakarta.mail.Transport.send(message, smtp.username(), smtp.password());
        } else {
            // Unauthenticated relay (e.g. an internal MTA on port 25): send with no
            // credentials. Passing empty username/password instead would make
            // jakarta.mail attempt an AUTH handshake the relay does not offer.
            jakarta.mail.Transport.send(message);
        }
    }

    /**
     * The SMTP transmission seam: builds and sends the HTML email, signalling failure by throwing (mirrors
     * {@code jakarta.mail}'s own {@code Transport.send}, which returns void and throws on failure). Injected so unit
     * tests assert the outcome without a real mail server.
     */
    @FunctionalInterface
    public interface Transmit {

        /**
         * @param recipients the recipient addresses
         * @param subject    the email subject
         * @param htmlBody   the HTML body
         * @throws EmailTransmitException on any transmission failure
         */
        void transmit(List<String> recipients, String subject, String htmlBody) throws EmailTransmitException;
    }

    /**
     * The non-secret SMTP connection facts plus the password. The password comes from the {@code SMTP_PASSWORD} env var
     * (never the properties file); the rest come from {@code notifications.properties}.
     */
    public record SmtpSettings(String host, String port, String tls, boolean auth, String from, String username,
            String password) {
    }

    /**
     * Thrown internally when an SMTP transmission fails. Caught by {@link #send} and mapped to
     * {@link NotificationOutcome#FAILED} — it never escapes the sender.
     */
    public static final class EmailTransmitException extends Exception {

        /**
         * @param message the failure description
         * @param cause   the underlying cause (may be {@code null})
         */
        public EmailTransmitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
