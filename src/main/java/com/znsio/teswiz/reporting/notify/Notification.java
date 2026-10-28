package com.znsio.teswiz.reporting.notify;

import java.util.List;

/**
 * A single, fully-resolved notification handed to the {@link Notifier} for delivery. teswiz treats it as opaque
 * content: it knows how to deliver to Slack and email, but nothing about what the message <em>means</em> — the client
 * composes the subject, Slack text and HTML body, and resolves the destinations. The {@code typeKey} is a free-form
 * label used only for logging and the per-target outcome map key; teswiz never branches on it.
 * <p>
 * A blank {@code slackWebhookUrl} skips Slack; an empty {@code emailRecipients} skips email — so a client can target
 * one channel, both, or (degenerate) neither, and the senders report {@link NotificationOutcome#SKIPPED_NO_DESTINATION}
 * for a missing destination.
 *
 * @param typeKey         a free-form label for this message (e.g. {@code "run"}); logging/outcome-key only
 * @param subject         the email subject (also used as the Slack message title)
 * @param slackText       the composed Slack message body
 * @param htmlBody        the self-contained HTML email body
 * @param slackWebhookUrl the resolved Slack incoming-webhook URL (blank → Slack skipped)
 * @param emailRecipients the resolved email recipients (empty → email skipped)
 */
public record Notification(String typeKey, String subject, String slackText, String htmlBody, String slackWebhookUrl,
        List<String> emailRecipients) {

    /**
     * Canonical constructor; defensively copies the recipient list and defaults a blank type key to {@code "run"}.
     */
    public Notification {
        typeKey = (typeKey == null || typeKey.isBlank()) ? "run" : typeKey;
        emailRecipients = emailRecipients == null ? List.of() : List.copyOf(emailRecipients);
    }
}
