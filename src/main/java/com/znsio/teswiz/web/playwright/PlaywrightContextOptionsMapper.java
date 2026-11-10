package com.znsio.teswiz.web.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.options.ViewportSize;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Translates the resolved browser-config {@code contextOptions} map into Playwright's
 * {@link Browser.NewContextOptions} for a Playwright Java session.
 *
 * <p>Absent keys leave Playwright's defaults untouched. String options are applied only when they
 * are non-blank strings. A {@code viewport} that is {@code null}, or a map without both
 * {@code width} and {@code height}, disables the fixed viewport (used for maximized windows).
 */
final class PlaywrightContextOptionsMapper {
    private static final String IGNORE_HTTPS_ERRORS = "ignoreHTTPSErrors";
    private static final String BASE_URL = "baseURL";
    private static final String VIEWPORT = "viewport";
    private static final String LOCALE = "locale";
    private static final String TIMEZONE_ID = "timezoneId";
    private static final String USER_AGENT = "userAgent";
    private static final String DEVICE_SCALE_FACTOR = "deviceScaleFactor";
    private static final String EXTRA_HTTP_HEADERS = "extraHTTPHeaders";

    private PlaywrightContextOptionsMapper() {
    }

    static Browser.NewContextOptions toNewContextOptions(Map<String, Object> contextOptions, Path harFile) {
        Browser.NewContextOptions options = new Browser.NewContextOptions();
        applyIgnoreHttpsErrors(contextOptions, options);
        applyNonBlankString(contextOptions, BASE_URL, options::setBaseURL);
        applyViewport(contextOptions, options);
        applyNonBlankString(contextOptions, LOCALE, options::setLocale);
        applyNonBlankString(contextOptions, TIMEZONE_ID, options::setTimezoneId);
        applyNonBlankString(contextOptions, USER_AGENT, options::setUserAgent);
        applyDeviceScaleFactor(contextOptions, options);
        applyExtraHttpHeaders(contextOptions, options);
        options.setRecordHarPath(harFile);
        return options;
    }

    private static void applyIgnoreHttpsErrors(Map<String, Object> contextOptions,
            Browser.NewContextOptions options) {
        if (contextOptions.containsKey(IGNORE_HTTPS_ERRORS)) {
            options.setIgnoreHTTPSErrors(Boolean.TRUE.equals(contextOptions.get(IGNORE_HTTPS_ERRORS)));
        }
    }

    private static void applyNonBlankString(Map<String, Object> contextOptions, String key,
            Consumer<String> setter) {
        if (contextOptions.get(key) instanceof String value && !value.isBlank()) {
            setter.accept(value);
        }
    }

    private static void applyViewport(Map<String, Object> contextOptions, Browser.NewContextOptions options) {
        if (!contextOptions.containsKey(VIEWPORT)) {
            return;
        }
        Object viewport = contextOptions.get(VIEWPORT);
        if (viewport instanceof Map<?, ?> viewportMap) {
            applyViewportDimensions(viewportMap, options);
        } else if (null == viewport) {
            disableFixedViewport(options);
        }
    }

    private static void applyViewportDimensions(Map<?, ?> viewportMap, Browser.NewContextOptions options) {
        Object width = viewportMap.get("width");
        Object height = viewportMap.get("height");
        if (null == width || null == height) {
            disableFixedViewport(options);
            return;
        }
        options.setViewportSize(toInt(width), toInt(height));
    }

    private static void disableFixedViewport(Browser.NewContextOptions options) {
        options.setViewportSize((ViewportSize) null);
    }

    private static void applyDeviceScaleFactor(Map<String, Object> contextOptions,
            Browser.NewContextOptions options) {
        Object deviceScaleFactor = contextOptions.get(DEVICE_SCALE_FACTOR);
        if (null != deviceScaleFactor) {
            options.setDeviceScaleFactor(Double.parseDouble(String.valueOf(deviceScaleFactor)));
        }
    }

    private static void applyExtraHttpHeaders(Map<String, Object> contextOptions,
            Browser.NewContextOptions options) {
        if (contextOptions.get(EXTRA_HTTP_HEADERS) instanceof Map<?, ?> rawHeaders) {
            Map<String, String> headers = new LinkedHashMap<>();
            rawHeaders.forEach((name, value) -> headers.put(String.valueOf(name), String.valueOf(value)));
            options.setExtraHTTPHeaders(headers);
        }
    }

    private static int toInt(Object value) {
        return Integer.parseInt(String.valueOf(value));
    }
}
