package com.znsio.teswiz.web.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.options.ViewportSize;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how the browser-config "contextOptions" map is translated into Playwright's
 * {@link Browser.NewContextOptions} for Playwright Java sessions.
 */
class PlaywrightContextOptionsMapperTest {
    private static final Path HAR_FILE = Path.of("target", "session.har");

    @Test
    void emptyContextOptionsOnlyRecordTheHarFile() {
        Browser.NewContextOptions options = map(Map.of());

        assertThat(options.recordHarPath).isEqualTo(HAR_FILE);
        assertThat(options.ignoreHTTPSErrors).isNull();
        assertThat(options.baseURL).isNull();
        assertThat(options.viewportSize).isNull();
        assertThat(options.locale).isNull();
        assertThat(options.timezoneId).isNull();
        assertThat(options.userAgent).isNull();
        assertThat(options.deviceScaleFactor).isNull();
        assertThat(options.extraHTTPHeaders).isNull();
    }

    @Test
    void ignoreHttpsErrorsIsTrueOnlyForBooleanTrue() {
        assertThat(map(Map.of("ignoreHTTPSErrors", true)).ignoreHTTPSErrors).isTrue();
        assertThat(map(Map.of("ignoreHTTPSErrors", false)).ignoreHTTPSErrors).isFalse();
        assertThat(map(Map.of("ignoreHTTPSErrors", "true")).ignoreHTTPSErrors).isFalse();
    }

    @Test
    void nonBlankStringOptionsAreApplied() {
        Browser.NewContextOptions options = map(Map.of(
                "baseURL", "https://example.com",
                "locale", "en-IN",
                "timezoneId", "Asia/Kolkata",
                "userAgent", "teswiz-agent"));

        assertThat(options.baseURL).isEqualTo("https://example.com");
        assertThat(options.locale).isEqualTo("en-IN");
        assertThat(options.timezoneId).isEqualTo("Asia/Kolkata");
        assertThat(options.userAgent).isEqualTo("teswiz-agent");
    }

    @Test
    void blankOrNonStringOptionsAreIgnored() {
        Browser.NewContextOptions options = map(Map.of(
                "baseURL", "  ",
                "locale", "",
                "timezoneId", 42,
                "userAgent", " "));

        assertThat(options.baseURL).isNull();
        assertThat(options.locale).isNull();
        assertThat(options.timezoneId).isNull();
        assertThat(options.userAgent).isNull();
    }

    @Test
    void viewportWithWidthAndHeightSetsAFixedViewport() {
        Browser.NewContextOptions options = map(Map.of("viewport", Map.of("width", 1440, "height", "900")));

        assertThat(options.viewportSize).isPresent();
        ViewportSize viewportSize = options.viewportSize.orElseThrow();
        assertThat(viewportSize.width).isEqualTo(1440);
        assertThat(viewportSize.height).isEqualTo(900);
    }

    @Test
    void viewportMissingADimensionDisablesTheFixedViewport() {
        Browser.NewContextOptions options = map(Map.of("viewport", Map.of("width", 1440)));

        assertThat(options.viewportSize).isEqualTo(Optional.empty());
    }

    @Test
    void nullViewportDisablesTheFixedViewport() {
        Map<String, Object> contextOptions = new HashMap<>();
        contextOptions.put("viewport", null);

        assertThat(map(contextOptions).viewportSize).isEqualTo(Optional.empty());
    }

    @Test
    void viewportOfAnUnexpectedTypeIsIgnored() {
        assertThat(map(Map.of("viewport", "1440x900")).viewportSize).isNull();
    }

    @Test
    void deviceScaleFactorIsParsedFromAnyValue() {
        assertThat(map(Map.of("deviceScaleFactor", 2)).deviceScaleFactor).isEqualTo(2.0);
        assertThat(map(Map.of("deviceScaleFactor", "1.5")).deviceScaleFactor).isEqualTo(1.5);
    }

    @Test
    void nullDeviceScaleFactorIsIgnored() {
        Map<String, Object> contextOptions = new HashMap<>();
        contextOptions.put("deviceScaleFactor", null);

        assertThat(map(contextOptions).deviceScaleFactor).isNull();
    }

    @Test
    void extraHttpHeadersAreCopiedAsStringsInOrder() {
        Map<Object, Object> headers = new LinkedHashMap<>();
        headers.put("X-Trace", 7);
        headers.put("X-Env", "staging");

        Browser.NewContextOptions options = map(Map.of("extraHTTPHeaders", headers));

        assertThat(options.extraHTTPHeaders).containsExactly(Map.entry("X-Trace", "7"), Map.entry("X-Env", "staging"));
    }

    @Test
    void extraHttpHeadersThatAreNotAMapAreIgnored() {
        assertThat(map(Map.of("extraHTTPHeaders", "X-Trace: 7")).extraHTTPHeaders).isNull();
    }

    private static Browser.NewContextOptions map(Map<String, Object> contextOptions) {
        return PlaywrightContextOptionsMapper.toNewContextOptions(contextOptions, HAR_FILE);
    }
}
