package com.znsio.teswiz.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.exceptions.InvalidTestDataException;

class TeswizRuntimeConfigurationTest {
    @AfterEach
    void resetRuntimeConfiguration() {
        System.clearProperty(TeswizRuntimeConfiguration.CONFIG_FILE);
        System.clearProperty(TeswizRuntimeConfiguration.DRIVER_WAIT_TIMEOUT_SECONDS);
        TeswizRuntimeConfiguration.load(null);
    }

    @Test
    void shouldLoadBuiltInDefaults() {
        TeswizRuntimeConfiguration.load(null);

        assertEquals(10, TeswizRuntimeConfiguration.getInt(
                TeswizRuntimeConfiguration.DRIVER_WAIT_TIMEOUT_SECONDS));
        assertEquals(15, TeswizRuntimeConfiguration.getInt(
                TeswizRuntimeConfiguration.APP_DOWNLOAD_TIMEOUT_SECONDS));
    }

    @Test
    void shouldLoadOverridesFromExternalPropertiesFile() throws IOException {
        Path runtimeConfig = Files.createTempFile("teswiz-runtime", ".properties");
        Files.writeString(runtimeConfig, TeswizRuntimeConfiguration.DRIVER_WAIT_TIMEOUT_SECONDS + "=22\n");
        System.setProperty(TeswizRuntimeConfiguration.CONFIG_FILE, runtimeConfig.toString());

        try {
            TeswizRuntimeConfiguration.load(new Properties());
            assertEquals(22, TeswizRuntimeConfiguration.getInt(
                    TeswizRuntimeConfiguration.DRIVER_WAIT_TIMEOUT_SECONDS));
        } finally {
            Files.deleteIfExists(runtimeConfig);
        }
    }

    @Test
    void shouldRejectInvalidRuntimeConfiguration() throws IOException {
        Path runtimeConfig = Files.createTempFile("teswiz-runtime", ".properties");
        Files.writeString(runtimeConfig, TeswizRuntimeConfiguration.DRIVER_VIEWPORT_WIDTH + "=0\n");

        try {
            Properties primaryConfiguration = new Properties();
            primaryConfiguration.put(TeswizRuntimeConfiguration.CONFIG_FILE, runtimeConfig.toString());
            assertThrows(InvalidTestDataException.class,
                    () -> TeswizRuntimeConfiguration.load(primaryConfiguration));
        } finally {
            Files.deleteIfExists(runtimeConfig);
        }
    }
}
