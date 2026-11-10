package com.znsio.teswiz.visual;

import com.applitools.eyes.MatchLevel;
import com.applitools.eyes.RectangleSize;
import com.znsio.teswiz.entities.APPLITOOLS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins how values are read from the loaded Applitools configuration map (applitools_config.json
 * plus teswiz overrides) when teswiz builds its Applitools set-up.
 */
class ApplitoolsConfigValuesTest {
    private static final int DEFAULT_WIDTH = 1280;
    private static final int DEFAULT_HEIGHT = 960;

    @Test
    void serverUrlIsTheConfiguredValueOrTheTextNull() {
        assertThat(ApplitoolsConfigValues.serverUrl(Map.of(APPLITOOLS.SERVER_URL, "https://eyes.example.com")))
                .isEqualTo("https://eyes.example.com");
        assertThat(ApplitoolsConfigValues.serverUrl(Map.of())).isEqualTo("null");
    }

    @Test
    void matchLevelIsTheConfiguredLevel() {
        assertThat(ApplitoolsConfigValues.matchLevel(Map.of(APPLITOOLS.DEFAULT_MATCH_LEVEL, "LAYOUT")))
                .isEqualTo(MatchLevel.LAYOUT);
        assertThat(ApplitoolsConfigValues.matchLevel(Map.of(APPLITOOLS.DEFAULT_MATCH_LEVEL, MatchLevel.CONTENT)))
                .isEqualTo(MatchLevel.CONTENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-level", "layout", ""})
    void unknownMatchLevelFallsBackToStrict(String configured) {
        assertThat(ApplitoolsConfigValues.matchLevel(Map.of(APPLITOOLS.DEFAULT_MATCH_LEVEL, configured)))
                .isEqualTo(MatchLevel.STRICT);
    }

    @Test
    void missingMatchLevelFallsBackToStrict() {
        assertThat(ApplitoolsConfigValues.matchLevel(Map.of())).isEqualTo(MatchLevel.STRICT);
    }

    @Test
    void viewportSizeIsParsedFromWidthByHeight() {
        RectangleSize size = ApplitoolsConfigValues.viewportSize(Map.of(APPLITOOLS.VIEWPORT_SIZE, "1440x900"),
                DEFAULT_WIDTH, DEFAULT_HEIGHT);

        assertThat(size.getWidth()).isEqualTo(1440);
        assertThat(size.getHeight()).isEqualTo(900);
    }

    @Test
    void missingViewportSizeUsesTheRuntimeDefaults() {
        RectangleSize size = ApplitoolsConfigValues.viewportSize(new HashMap<>(), DEFAULT_WIDTH, DEFAULT_HEIGHT);

        assertThat(size.getWidth()).isEqualTo(DEFAULT_WIDTH);
        assertThat(size.getHeight()).isEqualTo(DEFAULT_HEIGHT);
    }

    @Test
    void malformedViewportSizeIsNotSilentlyReplaced() {
        assertThatThrownBy(() -> ApplitoolsConfigValues.viewportSize(Map.of(APPLITOOLS.VIEWPORT_SIZE, "1440"),
                DEFAULT_WIDTH, DEFAULT_HEIGHT)).isInstanceOf(ArrayIndexOutOfBoundsException.class);
        assertThatThrownBy(() -> ApplitoolsConfigValues.viewportSize(Map.of(APPLITOOLS.VIEWPORT_SIZE, "axb"),
                DEFAULT_WIDTH, DEFAULT_HEIGHT)).isInstanceOf(NumberFormatException.class);
    }

    @Test
    void flagsAreTrueOnlyWhenConfiguredTrue() {
        Map<String, Object> enabled = Map.of(APPLITOOLS.ENABLE_BENCHMARK_PER_VALIDATION, true,
                APPLITOOLS.DISABLE_BROWSER_FETCHING, "TRUE");
        Map<String, Object> disabled = Map.of(APPLITOOLS.ENABLE_BENCHMARK_PER_VALIDATION, "no",
                APPLITOOLS.DISABLE_BROWSER_FETCHING, false);

        assertThat(ApplitoolsConfigValues.isBenchmarkingEnabled(enabled)).isTrue();
        assertThat(ApplitoolsConfigValues.isDisableBrowserFetching(enabled)).isTrue();
        assertThat(ApplitoolsConfigValues.isBenchmarkingEnabled(disabled)).isFalse();
        assertThat(ApplitoolsConfigValues.isDisableBrowserFetching(disabled)).isFalse();
        assertThat(ApplitoolsConfigValues.isBenchmarkingEnabled(Map.of())).isFalse();
        assertThat(ApplitoolsConfigValues.isDisableBrowserFetching(Map.of())).isFalse();
    }
}
