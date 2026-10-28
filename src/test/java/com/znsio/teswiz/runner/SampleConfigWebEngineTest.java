package com.znsio.teswiz.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class SampleConfigWebEngineTest {
    private static final String CONFIGS_DIR = "configs";
    private static final String WEB_ENGINE = "WEB_ENGINE";
    private static final String SELENIUM = "selenium";
    // End-of-run notification files (notify.* keys) are not teswiz execution configs, so they do not declare
    // WEB_ENGINE and are deliberately excluded from this execution-config check.
    private static final String NOTIFICATIONS_FILE_SUFFIX = "notifications.properties";

    @Test
    void shouldExplicitlySetWebEngineToSeleniumInAllCheckedInSampleConfigs() throws IOException {
        try (var paths = Files.walk(Path.of(CONFIGS_DIR))) {
            assertThat(paths.filter(this::isExecutionConfiguration))
                    .allSatisfy(this::assertWebEngineIsExplicitlySetToSelenium);
        }
    }

    private boolean isExecutionConfiguration(Path path) {
        String name = path.toString();
        return name.endsWith(".properties") && !name.endsWith(NOTIFICATIONS_FILE_SUFFIX);
    }

    private void assertWebEngineIsExplicitlySetToSelenium(Path configFile) {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(configFile)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read config file: " + configFile, e);
        }

        assertThat(properties)
                .as("Config file %s should declare WEB_ENGINE explicitly", configFile)
                .containsKey(WEB_ENGINE);
        assertThat(properties.getProperty(WEB_ENGINE))
                .as("Config file %s should default WEB_ENGINE to selenium", configFile)
                .isEqualTo(SELENIUM);
    }
}
