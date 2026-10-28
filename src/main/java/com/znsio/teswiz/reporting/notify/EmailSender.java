package com.znsio.teswiz.reporting.notify;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Sends an HTML email to a resolved recipient list over SMTP using {@code jakarta.mail}. Like the Slack sender, it
 * never throws from a delivery problem: an empty recipient list short-circuits to
 * {@link NotificationOutcome#SKIPPED_NO_DESTINATION}, and any transmit error is swallowed to
 * {@link NotificationOutcome#FAILED}. A notifier problem must never fail the run.
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
 * transitive) and an auto-organize-imports would otherwise resolve a short {@code Session} to the wrong type.
 */
public final class EmailSender {

    private static final Logger LOGGER = LogManager.getLogger(EmailSender.class.getName());

    private static final String HTML_UTF8 = "text/html; charset=utf-8";
    private static final String SMTP_TIMEOUT_MILLIS = "15000";

    private final Transmit transmit;

    /**
     * Creates a sender that transmits via {@code jakarta.mail} using the given SMTP settings.
     *
     * @param smtp the SMTP connection facts and the password
     */
    public EmailSender(SmtpSettings smtp) {
        this(defaultTransmit(smtp));
    }

    /**
     * Creates a sender with an injected transmit step — the unit-test seam.
     *
     * @param transmit the transmit implementation
     */
    public EmailSender(Transmit transmit) {
        this.transmit = transmit;
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
            Throwable cause = e.getCause();
            LOGGER.warn("Email delivery failed; swallowed so the run is unaffected: {} (cause: {})", e.getMessage(),
                    cause == null ? "none" : cause.toString());
            return NotificationOutcome.FAILED;
        }
    }

    private static Transmit defaultTransmit(SmtpSettings smtp) {
        return (recipients, subject, htmlBody) -> transmitViaJakarta(smtp, recipients, subject, htmlBody);
    }

    private static void transmitViaJakarta(SmtpSettings smtp, List<String> recipients, String subject, String htmlBody)
            throws EmailTransmitException {
        try {
            jakarta.mail.Session session = jakarta.mail.Session.getInstance(sessionProperties(smtp));
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
     * Maps the SMTP settings to a {@code jakarta.mail} session {@link Properties} — host, port, whether AUTH is
     * required, and whether STARTTLS is enabled (only when {@code tls} is {@code starttls}; a plaintext port-25 relay
     * leaves it off). Pure and side-effect-free; exposed so the config-to-session mapping can be asserted directly.
     *
     * @param smtp the SMTP connection facts
     * @return the jakarta.mail session properties
     */
    public static Properties sessionProperties(SmtpSettings smtp) {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", smtp.host());
        properties.put("mail.smtp.port", smtp.port());
        properties.put("mail.smtp.auth", Boolean.toString(smtp.auth()));
        properties.put("mail.smtp.starttls.enable", Boolean.toString("starttls".equalsIgnoreCase(smtp.tls())));
        // Bound every SMTP phase so a stalled relay fails fast instead of hanging the
        // notify task (which would otherwise stall the Gradle build until the OS timeout).
        properties.put("mail.smtp.connectiontimeout", SMTP_TIMEOUT_MILLIS);
        properties.put("mail.smtp.timeout", SMTP_TIMEOUT_MILLIS);
        properties.put("mail.smtp.writetimeout", SMTP_TIMEOUT_MILLIS);
        return properties;
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
