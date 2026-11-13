package com.znsio.teswiz.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AddressFallbackSettings}: whether teswiz connections try a host's other addresses when one
 * cannot be reached ({@code HTTP_ADDRESS_FALLBACK}, on by default), and how long each address gets to connect
 * ({@code HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS}, 5 by default). Like every teswiz setting, the value {@code Setup}
 * resolved (config file, overridden by environment or system property) wins; a stand-alone caller (no {@code Setup})
 * reads the environment and system properties directly. An unusable timeout keeps the default.
 */
class AddressFallbackSettingsTest {

    private static final Map<String, String> NOTHING = Map.of();

    private static AddressFallbackSettings resolve(Map<String, String> teswizConfig, Map<String, String> environment) {
        return AddressFallbackSettings.resolve(teswizConfig::get, environment::get);
    }

    @Test
    void nothingConfigured_meansFallbackIsOn_withFiveSecondsPerAddress() {
        AddressFallbackSettings settings = resolve(NOTHING, NOTHING);

        assertThat(settings.enabled()).isTrue();
        assertThat(settings.connectTimeoutPerAddress()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void theValuesSetupResolved_areUsed() {
        AddressFallbackSettings settings = resolve(
                Map.of("HTTP_ADDRESS_FALLBACK", "false", "HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS", "8"), NOTHING);

        assertThat(settings.enabled()).isFalse();
        assertThat(settings.connectTimeoutPerAddress()).isEqualTo(Duration.ofSeconds(8));
    }

    @Test
    void withoutSetup_theEnvironmentIsUsed() {
        AddressFallbackSettings settings = resolve(NOTHING,
                Map.of("HTTP_ADDRESS_FALLBACK", "false", "HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS", "10"));

        assertThat(settings.enabled()).isFalse();
        assertThat(settings.connectTimeoutPerAddress()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void theValueSetupResolved_winsOverTheEnvironment() {
        AddressFallbackSettings settings = resolve(Map.of("HTTP_ADDRESS_FALLBACK", "true"),
                Map.of("HTTP_ADDRESS_FALLBACK", "false"));

        assertThat(settings.enabled()).isTrue();
    }

    @Test
    void setupsPlaceholderForAMissingKey_isTreatedAsNotSet() {
        AddressFallbackSettings settings = resolve(
                Map.of("HTTP_ADDRESS_FALLBACK", "null", "HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS", "0"), NOTHING);

        assertThat(settings.enabled()).isTrue();
        assertThat(settings.connectTimeoutPerAddress()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void anUnusableTimeout_keepsTheDefault() {
        assertThat(resolve(NOTHING, Map.of("HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS", "soon"))
                .connectTimeoutPerAddress()).isEqualTo(Duration.ofSeconds(5));
        assertThat(resolve(NOTHING, Map.of("HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS", "-3"))
                .connectTimeoutPerAddress()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void enabledIsOnlyTurnedOffByAnExplicitFalse() {
        assertThat(resolve(NOTHING, Map.of("HTTP_ADDRESS_FALLBACK", "nope")).enabled()).isTrue();
        assertThat(resolve(NOTHING, Map.of("HTTP_ADDRESS_FALLBACK", "FALSE")).enabled()).isFalse();
    }
}
