package com.znsio.teswiz.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

/**
 * HEADLESS, IS_FAILING_TEST_SUITE and SET_HARD_GATE are active properties in the canonical
 * config template, so a value set in the config file must take effect. An environment variable
 * or system property still overrides the file, as for every other configuration property.
 */
class SetupConfigFileBooleansTest {
    private static final String BASE_CONFIG_FILE = "./configs/ajio/ajio_local_android_config.properties";

    @TempDir
    Path tempDir;

    @BeforeEach
    void clearOverrides() {
        clearSystemProperties();
    }

    @AfterEach
    void restoreDefaults() {
        clearSystemProperties();
        loadConfig(BASE_CONFIG_FILE);
    }

    @ParameterizedTest
    @ValueSource(strings = {Setup.HEADLESS, Setup.IS_FAILING_TEST_SUITE, Setup.SET_HARD_GATE})
    void shouldReadBooleanFromConfigFile(String key) throws IOException {
        loadConfig(configWith(key + "=true"));

        assertThat(Setup.getBooleanValueFromConfigs(key)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {Setup.HEADLESS, Setup.IS_FAILING_TEST_SUITE, Setup.SET_HARD_GATE})
    void shouldDefaultToFalseWhenConfigFileDoesNotSetIt(String key) {
        loadConfig(BASE_CONFIG_FILE);

        assertThat(Setup.getBooleanValueFromConfigs(key)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {Setup.HEADLESS, Setup.IS_FAILING_TEST_SUITE, Setup.SET_HARD_GATE})
    void shouldLetSystemPropertyOverrideConfigFile(String key) throws IOException {
        System.setProperty(key, "false");

        loadConfig(configWith(key + "=true"));

        assertThat(Setup.getBooleanValueFromConfigs(key)).isFalse();
    }

    private String configWith(String extraProperty) throws IOException {
        String baseConfig = Files.readString(Path.of(BASE_CONFIG_FILE));
        Path config = tempDir.resolve("config_with_override.properties");
        Files.writeString(config, baseConfig + System.lineSeparator() + extraProperty + System.lineSeparator());
        return config.toString();
    }

    private static void loadConfig(String configFile) {
        Setup.load(configFile);
        Setup.loadAndUpdateConfigParameters(configFile);
    }

    private static void clearSystemProperties() {
        System.clearProperty(Setup.HEADLESS);
        System.clearProperty(Setup.IS_FAILING_TEST_SUITE);
        System.clearProperty(Setup.SET_HARD_GATE);
    }
}
