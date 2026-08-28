package com.znsio.teswiz.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class ConfigurationTemplateTest {
    private static final Path TEMPLATE = Path.of("configs/teswiz/teswiz_config.properties.template");

    @Test
    void shouldDocumentEverySupportedExecutionProperty() throws IOException {
        String template = Files.readString(TEMPLATE);
        List<String> expectedProperties = List.of(
                "FRAMEWORK", "CAPS", "APP_NAME", "APP_PACKAGE_NAME", "APP_PATH", "BASE_URL_FOR_WEB", "BROWSER",
                "WEB_ENGINE", "PLATFORM", "ENVIRONMENT_CONFIG_FILE", "TARGET_ENVIRONMENT", "TEST_DATA_FILE", "TAG",
                "BRANCH_NAME", "BUILD_ID", "BUILD_INITIATION_REASON", "LOG_DIR", "LOG_PROPERTIES_FILE",
                "REPORT_PORTAL_FILE", "RP_DESCRIPTION", "LAUNCH_NAME_SUFFIX", "PARALLEL", "RUN_IN_CI",
                "IS_FAILING_TEST_SUITE", "SET_HARD_GATE", "HEADLESS", "IS_VISUAL", "FAIL_TEST_ON_VISUAL_DIFFERENCE",
                "APPLITOOLS_CONFIGURATION", "APPLITOOLS_API_KEY", "APPLITOOLS_BATCH_NAME_SUFFIX",
                "MAX_NUMBER_OF_APPIUM_DRIVERS", "MAX_NUMBER_OF_WEB_DRIVERS", "CLOUD_USERNAME", "CLOUD_KEY",
                "CLOUD_UPLOAD_APP", "CLOUD_USE_PROXY", "CLOUD_USE_LOCAL_TESTING", "PROXY_KEY", "PROXY_URL",
                "REMOTE_WEBDRIVER_GRID_HOST_NAME", "REMOTE_WEBDRIVER_GRID_PORT", "SHOW_SENSITIVE_DATA",
                "MASK_ADDITIONAL_KEYS", "MASK_KEYS_OVERRIDE", "TESWIZ_RUNTIME_CONFIG_FILE",
                "TESWIZ_DRIVER_WAIT_TIMEOUT_SECONDS", "TESWIZ_DRIVER_CLICK_RETRY_ATTEMPTS",
                "TESWIZ_DRIVER_CLICK_RETRY_DELAY_SECONDS", "TESWIZ_DRIVER_SCROLL_MAX_ATTEMPTS",
                "TESWIZ_DRIVER_VIEWPORT_WIDTH", "TESWIZ_DRIVER_VIEWPORT_HEIGHT",
                "TESWIZ_PLAYWRIGHT_PAGE_LOAD_TIMEOUT_SECONDS", "TESWIZ_PLAYWRIGHT_SCRIPT_TIMEOUT_SECONDS",
                "TESWIZ_MAX_NUMBER_OF_APPIUM_DRIVERS", "TESWIZ_MAX_NUMBER_OF_WEB_DRIVERS",
                "TESWIZ_APP_DOWNLOAD_TIMEOUT_SECONDS");

        assertThat(Files.exists(TEMPLATE)).isTrue();
        assertThat(expectedProperties).allSatisfy(property -> assertThat(template).contains(property + "="));
    }
}
