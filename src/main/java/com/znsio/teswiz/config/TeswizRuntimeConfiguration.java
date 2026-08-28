package com.znsio.teswiz.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.exceptions.InvalidTestDataException;

public final class TeswizRuntimeConfiguration {
    public static final String CONFIG_FILE = "TESWIZ_RUNTIME_CONFIG_FILE";
    public static final String DRIVER_WAIT_TIMEOUT_SECONDS = "TESWIZ_DRIVER_WAIT_TIMEOUT_SECONDS";
    public static final String DRIVER_CLICK_RETRY_ATTEMPTS = "TESWIZ_DRIVER_CLICK_RETRY_ATTEMPTS";
    public static final String DRIVER_CLICK_RETRY_DELAY_SECONDS = "TESWIZ_DRIVER_CLICK_RETRY_DELAY_SECONDS";
    public static final String DRIVER_SCROLL_MAX_ATTEMPTS = "TESWIZ_DRIVER_SCROLL_MAX_ATTEMPTS";
    public static final String DRIVER_VIEWPORT_WIDTH = "TESWIZ_DRIVER_VIEWPORT_WIDTH";
    public static final String DRIVER_VIEWPORT_HEIGHT = "TESWIZ_DRIVER_VIEWPORT_HEIGHT";
    public static final String PLAYWRIGHT_PAGE_LOAD_TIMEOUT_SECONDS = "TESWIZ_PLAYWRIGHT_PAGE_LOAD_TIMEOUT_SECONDS";
    public static final String PLAYWRIGHT_SCRIPT_TIMEOUT_SECONDS = "TESWIZ_PLAYWRIGHT_SCRIPT_TIMEOUT_SECONDS";
    public static final String MAX_NUMBER_OF_APPIUM_DRIVERS = "TESWIZ_MAX_NUMBER_OF_APPIUM_DRIVERS";
    public static final String MAX_NUMBER_OF_WEB_DRIVERS = "TESWIZ_MAX_NUMBER_OF_WEB_DRIVERS";
    public static final String APP_DOWNLOAD_TIMEOUT_SECONDS = "TESWIZ_APP_DOWNLOAD_TIMEOUT_SECONDS";

    private static final Logger LOGGER = LogManager.getLogger(TeswizRuntimeConfiguration.class.getName());
    private static final String DEFAULT_RESOURCE = "/defaultTeswizRuntime.properties";
    private static final int DEFAULT_WAIT_TIMEOUT_SECONDS = 10;
    private static final int DEFAULT_CLICK_RETRY_ATTEMPTS = 3;
    private static final int DEFAULT_CLICK_RETRY_DELAY_SECONDS = 1;
    private static final int DEFAULT_SCROLL_MAX_ATTEMPTS = 15;
    private static final int DEFAULT_VIEWPORT_WIDTH = 1280;
    private static final int DEFAULT_VIEWPORT_HEIGHT = 960;
    private static final int DEFAULT_PLAYWRIGHT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_MAX_DRIVERS = 5;
    private static final int DEFAULT_APP_DOWNLOAD_TIMEOUT_SECONDS = 15;
    private static final Set<String> POSITIVE_KEYS = Set.of(
            DRIVER_WAIT_TIMEOUT_SECONDS, DRIVER_SCROLL_MAX_ATTEMPTS, DRIVER_VIEWPORT_WIDTH,
            DRIVER_VIEWPORT_HEIGHT, PLAYWRIGHT_PAGE_LOAD_TIMEOUT_SECONDS, PLAYWRIGHT_SCRIPT_TIMEOUT_SECONDS,
            MAX_NUMBER_OF_APPIUM_DRIVERS, MAX_NUMBER_OF_WEB_DRIVERS, APP_DOWNLOAD_TIMEOUT_SECONDS);
    private static final Set<String> NON_NEGATIVE_KEYS = Set.of(
            DRIVER_CLICK_RETRY_ATTEMPTS, DRIVER_CLICK_RETRY_DELAY_SECONDS);
    private static final Map<String, String> BUILT_IN_DEFAULTS = Map.ofEntries(
            Map.entry(DRIVER_WAIT_TIMEOUT_SECONDS, String.valueOf(DEFAULT_WAIT_TIMEOUT_SECONDS)),
            Map.entry(DRIVER_CLICK_RETRY_ATTEMPTS, String.valueOf(DEFAULT_CLICK_RETRY_ATTEMPTS)),
            Map.entry(DRIVER_CLICK_RETRY_DELAY_SECONDS, String.valueOf(DEFAULT_CLICK_RETRY_DELAY_SECONDS)),
            Map.entry(DRIVER_SCROLL_MAX_ATTEMPTS, String.valueOf(DEFAULT_SCROLL_MAX_ATTEMPTS)),
            Map.entry(DRIVER_VIEWPORT_WIDTH, String.valueOf(DEFAULT_VIEWPORT_WIDTH)),
            Map.entry(DRIVER_VIEWPORT_HEIGHT, String.valueOf(DEFAULT_VIEWPORT_HEIGHT)),
            Map.entry(PLAYWRIGHT_PAGE_LOAD_TIMEOUT_SECONDS, String.valueOf(DEFAULT_PLAYWRIGHT_TIMEOUT_SECONDS)),
            Map.entry(PLAYWRIGHT_SCRIPT_TIMEOUT_SECONDS, String.valueOf(DEFAULT_PLAYWRIGHT_TIMEOUT_SECONDS)),
            Map.entry(MAX_NUMBER_OF_APPIUM_DRIVERS, String.valueOf(DEFAULT_MAX_DRIVERS)),
            Map.entry(MAX_NUMBER_OF_WEB_DRIVERS, String.valueOf(DEFAULT_MAX_DRIVERS)),
            Map.entry(APP_DOWNLOAD_TIMEOUT_SECONDS, String.valueOf(DEFAULT_APP_DOWNLOAD_TIMEOUT_SECONDS)));
    private static volatile Properties fileConfiguration = loadBuiltInDefaults();

    private TeswizRuntimeConfiguration() {
    }

    public static synchronized void load(Properties primaryConfiguration) {
        Properties loadedConfiguration = loadBuiltInDefaults();
        overlaySupportedRuntimeValues(loadedConfiguration, primaryConfiguration);
        String externalConfigFile = firstConfiguredValue(CONFIG_FILE, primaryConfiguration);
        if (externalConfigFile != null && !externalConfigFile.isBlank()) {
            try (InputStream input = new FileInputStream(externalConfigFile.trim())) {
                loadedConfiguration.putAll(loadProperties(input, externalConfigFile));
            } catch (IOException e) {
                throw new InvalidTestDataException("Unable to read runtime configuration file: " + externalConfigFile, e);
            }
        }
        validate(loadedConfiguration);
        fileConfiguration = loadedConfiguration;
    }

    public static int getInt(String key) {
        String configuredValue = firstConfiguredValue(key, fileConfiguration);
        if (configuredValue == null || configuredValue.isBlank()) {
            throw new InvalidTestDataException("Missing runtime configuration value: " + key);
        }
        return parseAndValidate(key, configuredValue);
    }

    private static Properties loadBuiltInDefaults() {
        Properties defaults = new Properties();
        defaults.putAll(BUILT_IN_DEFAULTS);
        try (InputStream input = TeswizRuntimeConfiguration.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input != null) {
                defaults.putAll(loadProperties(input, DEFAULT_RESOURCE));
            }
        } catch (IOException e) {
            throw new InvalidTestDataException("Unable to read built-in runtime configuration", e);
        }
        return defaults;
    }

    private static Properties loadProperties(InputStream input, String source) throws IOException {
        Properties loaded = new Properties();
        loaded.load(input);
        for (String key : loaded.stringPropertyNames()) {
            if (key.startsWith("TESWIZ_") && !BUILT_IN_DEFAULTS.containsKey(key)) {
                LOGGER.warn("Unknown runtime configuration key '{}' in {}", key, source);
            }
        }
        return loaded;
    }

    private static void overlaySupportedRuntimeValues(Properties target, Properties source) {
        if (source == null) {
            return;
        }
        for (String key : BUILT_IN_DEFAULTS.keySet()) {
            String value = source.getProperty(key);
            if (value != null) {
                target.setProperty(key, value);
            }
        }
    }

    private static void validate(Properties configuration) {
        for (String key : BUILT_IN_DEFAULTS.keySet()) {
            parseAndValidate(key, firstConfiguredValue(key, configuration));
        }
    }

    private static int parseAndValidate(String key, String value) {
        final int parsedValue;
        try {
            parsedValue = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw invalidValue(key, value, "an integer");
        }
        if (POSITIVE_KEYS.contains(key) && parsedValue <= 0) {
            throw invalidValue(key, value, "a positive integer");
        }
        if (NON_NEGATIVE_KEYS.contains(key) && parsedValue < 0) {
            throw invalidValue(key, value, "a non-negative integer");
        }
        return parsedValue;
    }

    private static InvalidTestDataException invalidValue(String key, String value, String expected) {
        return new InvalidTestDataException(
                String.format("Invalid runtime configuration: %s='%s'. Expected %s.", key, value, expected));
    }

    private static String firstConfiguredValue(String key, Properties primaryConfiguration) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        if ((value == null || value.isBlank()) && primaryConfiguration != null) {
            value = primaryConfiguration.getProperty(key);
        }
        return value;
    }
}
