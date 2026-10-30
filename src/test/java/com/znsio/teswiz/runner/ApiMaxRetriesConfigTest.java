package com.znsio.teswiz.runner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
/**
 * API_MAX_RETRIES (Playwright API engine attempts per call) must resolve like the other config keys:
 * env/system property first, then the config file, then the default of 3 - and never drop below 1.
 */
class ApiMaxRetriesConfigTest {
    private static final String SAMPLE_CONFIG = "./configs/ajio/ajio_local_android_config.properties";
    private static final String KEY = "API_MAX_RETRIES";
    @TempDir
    Path tempDir;
    @BeforeEach
    void clearOverride() {
        System.clearProperty(KEY);
    }
    @AfterEach
    void clearOverrideAfter() {
        System.clearProperty(KEY);
    }
    @Test
    void defaultsToThreeWhenNotConfigured() throws IOException {
        loadConfigWith(null);
        assertThat(Runner.getApiMaxRetries()).isEqualTo(3);
    }
    @Test
    void readsValueFromConfigFile() throws IOException {
        loadConfigWith(KEY + "=1");
        assertThat(Runner.getApiMaxRetries()).isEqualTo(1);
    }
    @Test
    void systemPropertyOverridesConfigFile() throws IOException {
        System.setProperty(KEY, "5");
        loadConfigWith(KEY + "=1");
        assertThat(Runner.getApiMaxRetries()).isEqualTo(5);
    }
    @Test
    void neverDropsBelowOneAttempt() throws IOException {
        loadConfigWith(KEY + "=0");
        assertThat(Runner.getApiMaxRetries()).isEqualTo(1);
    }
    private void loadConfigWith(String extraLine) throws IOException {
        String content = Files.readString(Path.of(SAMPLE_CONFIG));
        if (extraLine != null) {
            content = content + System.lineSeparator() + extraLine + System.lineSeparator();
        }
        Path configFile = Files.writeString(tempDir.resolve("config.properties"), content);
        Setup.load(configFile.toString());
        Setup.loadAndUpdateConfigParameters(configFile.toString());
    }
}
