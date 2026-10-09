package com.znsio.teswiz.config.browser;

import org.openqa.selenium.Dimension;

import com.znsio.teswiz.config.TeswizRuntimeConfiguration;

/**
 * Single source of truth for the default browser window / viewport dimensions
 * shared across web engines (Selenium and Playwright).
 *
 * <p>Before this resolver, Selenium hard-coded a {@code 1920x1080} fallback while
 * Playwright read {@code TESWIZ_DRIVER_VIEWPORT_WIDTH}/{@code HEIGHT} from
 * {@link TeswizRuntimeConfiguration}. Both engines now resolve the same
 * configured values here, so a sizing override applies consistently everywhere.
 */
public final class WindowSizingResolver {

    private WindowSizingResolver() {
    }

    public static int defaultWidth() {
        return TeswizRuntimeConfiguration.getInt(TeswizRuntimeConfiguration.DRIVER_VIEWPORT_WIDTH);
    }

    public static int defaultHeight() {
        return TeswizRuntimeConfiguration.getInt(TeswizRuntimeConfiguration.DRIVER_VIEWPORT_HEIGHT);
    }

    public static Dimension defaultViewport() {
        return new Dimension(defaultWidth(), defaultHeight());
    }
}
