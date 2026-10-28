package com.znsio.teswiz.exceptions;

/**
 * Thrown when notification configuration cannot be loaded (missing or unreadable {@code notifications.properties}), or
 * — only when {@code notify.fail.on.error=true} — when a run wanted to notify but could not (a delivery failure, a
 * missing destination for an enabled channel, or a broken composer).
 * <p>
 * By default ({@code notify.fail.on.error=false}) delivery failures (Slack/SMTP errors) are NOT signalled with this
 * exception — they are swallowed so a transient notification problem never fails a test run. The opt-in fail-fast
 * policy that escalates them lives in {@code RunNotifications}; the senders themselves still never throw.
 */
public class NotificationException extends RuntimeException {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
