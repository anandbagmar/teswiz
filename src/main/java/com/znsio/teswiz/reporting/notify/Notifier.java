package com.znsio.teswiz.reporting.notify;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Delivers composed {@link Notification}s to Slack and/or email, applying only the generic gates teswiz owns — the
 * master kill-switch ({@code notify.enabled}) and the branch allowlist ({@code notify.branches}) — then routing each
 * message to {@link SlackSender} / {@link EmailSender} by whether it carries a destination. teswiz knows nothing about
 * what a message means; the client decided its content and destinations. A notifier problem is never allowed to escape:
 * the senders swallow delivery failures into a {@link NotificationOutcome}, and this class never throws.
 * <p>
 * Per-"type" enable toggles are NOT teswiz's concern — a consuming project that wants them simply does not build a
 * {@link Notification} for a type it has turned off. teswiz gates only on the global switch and the branch.
 */
public final class Notifier {

    private static final Logger LOGGER = LogManager.getLogger(Notifier.class.getName());

    private static final String ENABLED_KEY = "notify.enabled";
    private static final String BRANCHES_KEY = "notify.branches";
    private static final String SMTP_HOST_KEY = "notify.smtp.host";
    private static final String SMTP_PORT_KEY = "notify.smtp.port";
    private static final String SMTP_TLS_KEY = "notify.smtp.tls";
    private static final String SMTP_AUTH_KEY = "notify.smtp.auth";
    private static final String SMTP_FROM_KEY = "notify.smtp.from";
    private static final String SMTP_USERNAME_KEY = "notify.smtp.username";
    private static final String SMTP_PASSWORD_ENV = "SMTP_PASSWORD";

    private final NotificationSettings settings;
    private final String currentBranch;
    private final SlackSender slackSender;
    private final EmailSender emailSender;

    /**
     * Creates a notifier using the default JDK Slack transport and {@code jakarta.mail} SMTP transport built from the
     * settings.
     *
     * @param settings      the resolved notification settings (gate keys + SMTP facts)
     * @param currentBranch the branch the run is on, for the branch gate
     */
    public Notifier(NotificationSettings settings, String currentBranch) {
        this(settings, currentBranch, new SlackSender(), new EmailSender(smtpSettings(settings)));
    }

    /**
     * Creates a notifier with injected senders — the unit-test seam.
     *
     * @param settings      the resolved notification settings (gate keys)
     * @param currentBranch the branch the run is on, for the branch gate
     * @param slackSender   the Slack sender (its HTTP POST may be injected)
     * @param emailSender   the email sender (its transmit step may be injected)
     */
    public Notifier(NotificationSettings settings, String currentBranch, SlackSender slackSender,
            EmailSender emailSender) {
        this.settings = settings;
        this.currentBranch = currentBranch;
        this.slackSender = slackSender;
        this.emailSender = emailSender;
    }

    /**
     * Gates on the master switch + branch allowlist, then delivers each notification. Returns a per-target outcome map
     * keyed {@code "<typeKey>.<channel>"}; empty when a gate blocked the run or there was nothing to send. Never throws.
     *
     * @param notifications the composed messages to deliver
     * @return the per-target outcomes
     */
    public Map<String, NotificationOutcome> sendAll(List<Notification> notifications) {
        Map<String, NotificationOutcome> outcomes = new LinkedHashMap<>();
        if (notifications == null || notifications.isEmpty()) {
            return outcomes;
        }
        if (gatesClosed()) {
            return outcomes;
        }
        for (Notification notification : notifications) {
            deliver(notification, outcomes);
        }
        return outcomes;
    }

    private void deliver(Notification notification, Map<String, NotificationOutcome> outcomes) {
        if (!notification.slackWebhookUrl().isBlank()) {
            outcomes.put(notification.typeKey() + "." + NotificationChannel.SLACK.key(),
                    slackSender.send(notification.slackWebhookUrl(), notification.slackText()));
        }
        if (!notification.emailRecipients().isEmpty()) {
            outcomes.put(notification.typeKey() + "." + NotificationChannel.EMAIL.key(), emailSender
                    .send(notification.emailRecipients(), notification.subject(), notification.htmlBody()));
        }
    }

    private boolean gatesClosed() {
        if (!settings.isTrue(ENABLED_KEY)) {
            LOGGER.info("Notifications disabled (notify.enabled is off); nothing sent.");
            return true;
        }
        List<String> allowlist = settings.getList(BRANCHES_KEY);
        BranchGate gate = new BranchGate(allowlist.isEmpty() ? List.of("master") : allowlist);
        if (!gate.allows(currentBranch)) {
            LOGGER.info("Branch '{}' is not in the notify allowlist; nothing sent.", currentBranch);
            return true;
        }
        return false;
    }

    private static EmailSender.SmtpSettings smtpSettings(NotificationSettings settings) {
        return new EmailSender.SmtpSettings(settings.get(SMTP_HOST_KEY), settings.get(SMTP_PORT_KEY),
                settings.get(SMTP_TLS_KEY), settings.isTrue(SMTP_AUTH_KEY), settings.get(SMTP_FROM_KEY),
                settings.get(SMTP_USERNAME_KEY), settings.env(SMTP_PASSWORD_ENV));
    }
}
