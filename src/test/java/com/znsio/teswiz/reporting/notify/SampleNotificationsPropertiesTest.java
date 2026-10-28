package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Guards the shared sample {@code notifications.properties} (on the test classpath root): it must load cleanly and
 * stay <em>disabled</em>, so sample runs never attempt to deliver a notification. If someone accidentally flips the
 * samples on (or breaks the resource), this fails loudly rather than letting the samples start POSTing to Slack/SMTP.
 */
class SampleNotificationsPropertiesTest {

    private static final String RESOURCE = "notifications.properties";

    @Test
    void theSampleResourceLoadsFromTheClasspath() {
        NotificationSettings settings = NotificationSettings.load(RESOURCE, Map.of());

        assertThat(settings).isNotNull();
    }

    @Test
    void notificationsAreDisabledForTheSamples() {
        NotificationSettings settings = NotificationSettings.load(RESOURCE, Map.of());

        assertThat(settings.isTrue("notify.enabled")).isFalse();
        assertThat(settings.isTrue("notify.slack.enabled")).isFalse();
        assertThat(settings.isTrue("notify.email.enabled")).isFalse();
    }

    @Test
    void noComposerIsConfiguredSoTheSamplesNeverCompose() {
        NotificationSettings settings = NotificationSettings.load(RESOURCE, Map.of());

        assertThat(settings.get("notify.composer.class")).isEmpty();
    }

    @Test
    void failOnErrorIsOffSoASampleRunIsNeverFailedByNotifications() {
        NotificationSettings settings = NotificationSettings.load(RESOURCE, Map.of());

        assertThat(settings.isTrue("notify.fail.on.error")).isFalse();
    }
}
