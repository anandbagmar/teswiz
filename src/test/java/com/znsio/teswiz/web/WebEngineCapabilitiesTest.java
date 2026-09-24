package com.znsio.teswiz.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WebEngineCapabilitiesTest {

    @Test
    void shouldReportFullCapabilitiesForSeleniumAndPlaywrightTs() {
        assertThat(WebEngineCapabilities.supports(WebEngine.SELENIUM, WebCapability.FRAMES)).isTrue();
        assertThat(WebEngineCapabilities.supports(WebEngine.SELENIUM, WebCapability.COOKIE_MANAGEMENT)).isTrue();

        assertThat(WebEngineCapabilities.supports(WebEngine.PLAYWRIGHT_TS, WebCapability.FRAMES)).isTrue();
        assertThat(WebEngineCapabilities.supports(WebEngine.PLAYWRIGHT_TS, WebCapability.ASYNC_SCRIPT)).isTrue();
    }

    @Test
    void shouldReportGapsAndFormatActionableDiagnosticMessageForPlaywrightJava() {
        assertThat(WebEngineCapabilities.supports(WebEngine.PLAYWRIGHT_JAVA, WebCapability.FRAMES)).isTrue();
        assertThat(WebEngineCapabilities.supports(WebEngine.PLAYWRIGHT_JAVA, WebCapability.WINDOW_POSITION)).isFalse();

        String message = WebEngineCapabilities.formatDiagnosticMessage(WebEngine.PLAYWRIGHT_JAVA, WebCapability.FRAMES, "frame(int)");

        assertThat(message).contains("Capability : FRAMES");
        assertThat(message).contains("WEB_ENGINE=playwright-java");
        assertThat(message).contains("HOW TO RESOLVE:");
        assertThat(message).contains("Use context.page().frameLocator");
    }
}
