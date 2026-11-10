package com.znsio.teswiz.visual;

import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.RectangleSize;
import com.znsio.teswiz.entities.APPLITOOLS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Reads typed values from the loaded Applitools configuration map (applitools_config.json plus
 * teswiz overrides). Pure functions over the map, so they can be tested without loading teswiz.
 */
public final class ApplitoolsConfigValues {
    private static final Logger LOGGER = LogManager.getLogger(ApplitoolsConfigValues.class.getName());

    private ApplitoolsConfigValues() {
    }

    /** The configured server URL, or the text {@code "null"} when none is configured. */
    public static String serverUrl(Map<?, ?> applitoolsConfiguration) {
        return String.valueOf(applitoolsConfiguration.get(APPLITOOLS.SERVER_URL));
    }

    /** The configured default match level (case-sensitive), or {@link MatchLevel#STRICT}. */
    public static MatchLevel matchLevel(Map<?, ?> applitoolsConfiguration) {
        try {
            return MatchLevel.valueOf(String.valueOf(applitoolsConfiguration.get(APPLITOOLS.DEFAULT_MATCH_LEVEL)));
        } catch (IllegalArgumentException | NullPointerException e) {
            return MatchLevel.STRICT;
        }
    }

    /**
     * The configured {@code "<width>x<height>"} viewport, or the given defaults when none is
     * configured. A malformed value is not replaced: it fails, so the misconfiguration is visible.
     */
    public static RectangleSize viewportSize(Map<?, ?> applitoolsConfiguration, int defaultWidth,
            int defaultHeight) {
        try {
            String[] viewP = ((String) applitoolsConfiguration.get(APPLITOOLS.VIEWPORT_SIZE)).split("x");
            return new RectangleSize(Integer.parseInt(viewP[0]), Integer.parseInt(viewP[1]));
        } catch (NullPointerException e) {
            LOGGER.info("Unable to get viewport size from Applitools configuration. Using runtime defaults.");
            return new RectangleSize(defaultWidth, defaultHeight);
        }
    }

    public static boolean isBenchmarkingEnabled(Map<?, ?> applitoolsConfiguration) {
        return Boolean.parseBoolean(
                String.valueOf(applitoolsConfiguration.get(APPLITOOLS.ENABLE_BENCHMARK_PER_VALIDATION)));
    }

    public static boolean isDisableBrowserFetching(Map<?, ?> applitoolsConfiguration) {
        return Boolean.parseBoolean(String.valueOf(applitoolsConfiguration.get(APPLITOOLS.DISABLE_BROWSER_FETCHING)));
    }
}
