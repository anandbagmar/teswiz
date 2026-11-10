package com.znsio.teswiz.config;

/**
 * Read-only view of the teswiz execution configuration (the values loaded from the config file,
 * environment variables and system properties). Library packages read configuration through
 * {@link TeswizConfig#reader()} instead of {@code com.znsio.teswiz.runner.Setup}, so they do not
 * depend on the {@code runner} package.
 *
 * <p>Missing keys never throw: strings are {@code null}, booleans {@code false}, integers {@code 0}.
 */
public interface TeswizConfigReader {

    String getString(String key);

    String getStringOrDefault(String key, String defaultValue);

    boolean getBoolean(String key);

    /** {@code "true"}/{@code "false"}, or {@code "null"} when the key is missing. */
    String getBooleanAsString(String key);

    int getInteger(String key);

    String getIntegerAsString(String key);
}
