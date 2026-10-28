package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Guards the default sample notifications file at {@code configs/teswiz/notifications.properties} — the path every
 * example {@code config.properties} sets {@code NOTIFICATIONS_CONFIG_FILE} to. It must load cleanly from disk and stay
 * <em>disabled</em>, so sample runs never attempt to deliver a notification. If someone accidentally flips the samples
 * on (or breaks the file), this fails loudly rather than letting the samples start POSTing to Slack/SMTP.
 */
class SampleNotificationsPropertiesTest {

    private static final Path SAMPLE_FILE = Path.of("configs", "teswiz", "notifications.properties");

    @Test
    void theSampleFileLoadsFromTheConfiguredPath() {
        NotificationSettings settings = NotificationSettings.loadFromFile(SAMPLE_FILE, Map.of());

        assertThat(settings).isNotNull();
    }

    @Test
    void notificationsAreDisabledForTheSamples() {
        NotificationSettings settings = NotificationSettings.loadFromFile(SAMPLE_FILE, Map.of());

        assertThat(settings.isTrue("notify.enabled")).isFalse();
        assertThat(settings.isTrue("notify.slack.enabled")).isFalse();
        assertThat(settings.isTrue("notify.email.enabled")).isFalse();
    }

    @Test
    void noComposerIsConfiguredSoTheSamplesNeverCompose() {
        NotificationSettings settings = NotificationSettings.loadFromFile(SAMPLE_FILE, Map.of());

        assertThat(settings.get("notify.composer.class")).isEmpty();
    }

    @Test
    void failOnErrorIsOffSoASampleRunIsNeverFailedByNotifications() {
        NotificationSettings settings = NotificationSettings.loadFromFile(SAMPLE_FILE, Map.of());

        assertThat(settings.isTrue("notify.fail.on.error")).isFalse();
    }
}
