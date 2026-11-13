package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for how {@link Notifier} reads the Slack and SMTP timeouts from {@code notifications.properties}
 * ({@code notify.slack.timeout.seconds}, {@code notify.smtp.timeout.seconds}). A missing, blank, non-numeric or
 * non-positive value falls back to the sender's default, so a bad setting never stops a notification from being sent.
 */
class NotifierTimeoutSettingsTest {

    private static final Duration DEFAULT = Duration.ofSeconds(20);
    private static final String SLACK_KEY = "notify.slack.timeout.seconds";

    @TempDir
    Path tempDir;

    private NotificationSettings settingsWith(String fileContent, Map<String, String> environment) throws IOException {
        Path file = tempDir.resolve("notifications.properties");
        Files.writeString(file, fileContent);
        return NotificationSettings.loadFromFile(file, environment);
    }

    @Test
    void aConfiguredTimeoutIsUsed() throws IOException {
        NotificationSettings settings = settingsWith(SLACK_KEY + "=45\n", Map.of());

        assertThat(Notifier.timeout(settings, SLACK_KEY, DEFAULT)).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void theEnvironmentOverridesTheFile() throws IOException {
        NotificationSettings settings = settingsWith(SLACK_KEY + "=45\n",
                Map.of("NOTIFY_SLACK_TIMEOUT_SECONDS", "60"));

        assertThat(Notifier.timeout(settings, SLACK_KEY, DEFAULT)).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void aMissingTimeoutUsesTheDefault() throws IOException {
        NotificationSettings settings = settingsWith("", Map.of());

        assertThat(Notifier.timeout(settings, SLACK_KEY, DEFAULT)).isEqualTo(DEFAULT);
    }

    @Test
    void aNonNumericTimeoutUsesTheDefault() throws IOException {
        NotificationSettings settings = settingsWith(SLACK_KEY + "=soon\n", Map.of());

        assertThat(Notifier.timeout(settings, SLACK_KEY, DEFAULT)).isEqualTo(DEFAULT);
    }

    @Test
    void aZeroOrNegativeTimeoutUsesTheDefault() throws IOException {
        NotificationSettings zero = settingsWith(SLACK_KEY + "=0\n", Map.of());
        NotificationSettings negative = settingsWith(SLACK_KEY + "=-5\n", Map.of());

        assertThat(Notifier.timeout(zero, SLACK_KEY, DEFAULT)).isEqualTo(DEFAULT);
        assertThat(Notifier.timeout(negative, SLACK_KEY, DEFAULT)).isEqualTo(DEFAULT);
    }
}
