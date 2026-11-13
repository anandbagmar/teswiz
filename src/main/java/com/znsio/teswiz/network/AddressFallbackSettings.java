package com.znsio.teswiz.network;

import java.time.Duration;
import java.util.function.Function;

import com.znsio.teswiz.config.TeswizConfig;
import com.znsio.teswiz.tools.OverriddenVariable;

/**
 * Whether teswiz connections try a host's other addresses when one cannot be reached, and how long each address gets
 * to connect.
 * <ul>
 * <li>{@code HTTP_ADDRESS_FALLBACK} - on unless set to {@code false}</li>
 * <li>{@code HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS} - 5 unless set to a positive whole number</li>
 * </ul>
 * Like every teswiz setting, the value {@code Setup} resolved (config file, overridden by environment or system
 * property) wins; a stand-alone caller (no {@code Setup}) reads the environment and system properties directly.
 *
 * @param enabled                  whether to try the next address when one cannot be reached
 * @param connectTimeoutPerAddress how long each address gets to connect
 */
public record AddressFallbackSettings(boolean enabled, Duration connectTimeoutPerAddress) {

    public static final String ENABLED_KEY = "HTTP_ADDRESS_FALLBACK";
    public static final String TIMEOUT_KEY = "HTTP_CONNECT_TIMEOUT_PER_ADDRESS_SECONDS";
    public static final Duration DEFAULT_CONNECT_TIMEOUT_PER_ADDRESS = Duration.ofSeconds(5);
    // Setup answers these for a key it does not have (getBooleanAsString / getIntegerAsString)
    private static final String MISSING_BOOLEAN = "null";
    private static final String MISSING_INTEGER = "0";

    /** @return the configured settings (through {@code Setup}, or the environment when it has not run) */
    public static AddressFallbackSettings fromEnvironment() {
        return resolve(AddressFallbackSettings::teswizConfigValue,
                key -> OverriddenVariable.getOverriddenStringValue(key, null));
    }

    /**
     * @param teswizConfig the configuration {@code Setup} has resolved ({@code null} for every key when it has not run)
     * @param environment  environment variables and system properties
     * @return the settings, with the default for anything not set or unusable
     */
    public static AddressFallbackSettings resolve(Function<String, String> teswizConfig,
            Function<String, String> environment) {
        String enabled = firstSet(teswizConfig.apply(ENABLED_KEY), environment.apply(ENABLED_KEY));
        String timeout = firstSet(teswizConfig.apply(TIMEOUT_KEY), environment.apply(TIMEOUT_KEY));
        return new AddressFallbackSettings(!"false".equalsIgnoreCase(enabled), timeoutFrom(timeout));
    }

    private static String teswizConfigValue(String key) {
        return ENABLED_KEY.equals(key) ? TeswizConfig.reader().getBooleanAsString(key)
                : TeswizConfig.reader().getIntegerAsString(key);
    }

    private static String firstSet(String fromSetup, String fromEnvironment) {
        if (isSet(fromSetup)) {
            return fromSetup.trim();
        }
        return isSet(fromEnvironment) ? fromEnvironment.trim() : null;
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank() && !MISSING_BOOLEAN.equals(value.trim())
                && !MISSING_INTEGER.equals(value.trim());
    }

    private static Duration timeoutFrom(String seconds) {
        if (seconds == null) {
            return DEFAULT_CONNECT_TIMEOUT_PER_ADDRESS;
        }
        try {
            int parsed = Integer.parseInt(seconds);
            return parsed > 0 ? Duration.ofSeconds(parsed) : DEFAULT_CONNECT_TIMEOUT_PER_ADDRESS;
        } catch (NumberFormatException e) {
            return DEFAULT_CONNECT_TIMEOUT_PER_ADDRESS;
        }
    }
}
