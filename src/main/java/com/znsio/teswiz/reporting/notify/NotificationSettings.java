package com.znsio.teswiz.reporting.notify;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.exceptions.NotificationException;

/**
 * A generic notification settings loader: reads a {@code .properties} resource from the classpath and layers an
 * environment override on top of every key — an env value always wins over the file value, so a project ships safe
 * defaults in the file and CI flips switches via same-named environment variables. Property keys map to env var names
 * by upper-snake-casing and replacing dots with underscores (e.g. {@code notify.enabled} → {@code NOTIFY_ENABLED}).
 * <p>
 * This class is deliberately domain-agnostic: it knows how to resolve a key to a value / flag / list, and nothing
 * about what the keys <em>mean</em>. teswiz uses it to read the channel + gate keys it owns ({@code notify.enabled},
 * {@code notify.branches}, {@code notify.email.*}, {@code notify.slack.*}); a consuming project can use the same loader
 * for its own keys. Secrets are never read from the file — callers read those from the environment directly.
 */
public final class NotificationSettings {

    private static final Logger LOGGER = LogManager.getLogger(NotificationSettings.class.getName());

    private static final String TRUE = "true";
    private static final String LIST_SEPARATOR = ",";

    private final Properties fileProperties;
    private final Map<String, String> environment;

    private NotificationSettings(Properties fileProperties, Map<String, String> environment) {
        this.fileProperties = fileProperties;
        this.environment = environment;
    }

    /**
     * Loads settings from a classpath properties resource using the real process environment for overrides.
     *
     * @param resourceName the classpath properties resource to read
     * @return the loaded settings
     */
    public static NotificationSettings load(String resourceName) {
        return load(resourceName, System.getenv());
    }

    /**
     * Loads settings from a classpath properties resource against an explicit environment — the unit-test seam that
     * makes the file-vs-env decision deterministic regardless of the ambient CI environment.
     *
     * @param resourceName the classpath properties resource to read
     * @param environment  the environment variables to consult for overrides
     * @return the loaded settings
     */
    public static NotificationSettings load(String resourceName, Map<String, String> environment) {
        return new NotificationSettings(loadProperties(resourceName), environment);
    }

    /**
     * Resolves a key to its string value (env override wins over the file), trimmed; empty string when unset.
     *
     * @param key the property key
     * @return the resolved value, or an empty string
     */
    public String get(String key) {
        String fromEnv = env(toEnvName(key));
        if (!fromEnv.isEmpty()) {
            return fromEnv;
        }
        return fileProperties.getProperty(key, "").trim();
    }

    /**
     * Resolves a key to a boolean flag — {@code true} only when the resolved value equals {@code "true"}
     * (case-insensitive).
     *
     * @param key the property key
     * @return the resolved flag
     */
    public boolean isTrue(String key) {
        return TRUE.equalsIgnoreCase(get(key));
    }

    /**
     * Resolves a key to a comma-separated list, trimming each element and dropping empties.
     *
     * @param key the property key
     * @return the resolved values (possibly empty, never {@code null})
     */
    public List<String> getList(String key) {
        String rawValue = get(key);
        if (rawValue.isBlank()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : rawValue.split(LIST_SEPARATOR)) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    /**
     * Reads an environment variable directly (never the file) — for secrets such as a webhook URL or SMTP password.
     *
     * @param envName the environment variable name
     * @return the value, or an empty string when unset
     */
    public String env(String envName) {
        String value = environment.get(envName);
        return value == null ? "" : value.trim();
    }

    private static String toEnvName(String propertyKey) {
        return propertyKey.toUpperCase().replace('.', '_');
    }

    private static Properties loadProperties(String resourceName) {
        Properties properties = new Properties();
        try (InputStream stream = NotificationSettings.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new NotificationException(resourceName + " not found on the classpath. "
                        + "Create a " + resourceName + " resource with the notification settings.");
            }
            properties.load(stream);
            LOGGER.debug("Loaded {} notification settings from {}", properties.size(), resourceName);
        } catch (IOException e) {
            throw new NotificationException("Failed to load " + resourceName, e);
        }
        return properties;
    }
}
