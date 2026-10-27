package com.znsio.teswiz.reporting.notify;

/**
 * The delivery channels teswiz can send a {@link Notification} over.
 * <p>
 * The lower-case {@link #key()} is the token used to compose the per-target outcome map key and the channel property
 * keys teswiz reads (e.g. {@code notify.slack.enabled} / {@code notify.email.enabled}).
 */
public enum NotificationChannel {

    SLACK, EMAIL;

    /**
     * The lower-case token for this channel, used to compose property keys and env var names.
     *
     * @return the channel key, e.g. {@code "slack"}
     */
    public String key() {
        return name().toLowerCase();
    }
}
