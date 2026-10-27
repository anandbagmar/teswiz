package com.znsio.teswiz.reporting.notify;

/**
 * The observable result of attempting to deliver one notification over one channel. A sender returns a value instead of
 * relying on a void + side effects — and a delivery problem is reported as an outcome, never thrown. A notifier failure
 * never changes the test or pipeline result.
 */
public enum NotificationOutcome {

    /** Delivered successfully (Slack/SMTP accepted the message). */
    SENT,
    /** The type x channel switch was off, so nothing was attempted. */
    SKIPPED_DISABLED,
    /** The switch was on but no destination was configured (missing webhook or empty recipient list). */
    SKIPPED_NO_DESTINATION,
    /** Delivery was attempted and failed; the failure was swallowed and recorded here. */
    FAILED
}
