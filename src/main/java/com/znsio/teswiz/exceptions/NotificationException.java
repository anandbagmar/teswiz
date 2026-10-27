package com.znsio.teswiz.exceptions;

/**
 * Thrown when notification configuration cannot be loaded (missing or unreadable {@code notifications.properties}).
 * <p>
 * Delivery failures (Slack/SMTP errors) are NOT signalled with this exception - they are swallowed by the senders so a
 * notification problem never fails a test run. This exception is reserved for a genuinely broken setup surfaced at load
 * time.
 */
public class NotificationException extends RuntimeException {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
