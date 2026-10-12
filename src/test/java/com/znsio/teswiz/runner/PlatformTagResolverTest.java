package com.znsio.teswiz.runner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.znsio.teswiz.entities.Platform;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PlatformTagResolver}, which replaced the multi-user tag {@code if/else}
 * chain in {@link Setup}. Locks the tag -> (Platform, launch-name suffix) mapping and the
 * order-sensitive precedence that substring matching depends on.
 */
class PlatformTagResolverTest {

    @ParameterizedTest
    @CsvSource({
            "@multiuser-android-web, android, ' - Real User Simulation on Android & Web'",
            "@multiuser-android, android, ' - Real User Simulation on multiple Androids'",
            "@multiuser-web, web, ' - Real User Simulation on Web'",
            "@multiuser-electron, electron, ' - Real User Simulation on Electron'",
            "@multiuser-windows-web, windows, ' - Real User Simulation on Windows & Web'",
            "@multiuser-windows-android, windows, ' - Real User Simulation on Windows & Android'",
            "@multiuser-iOS, iOS, ' - Real User Simulation on IOS'"
    })
    void resolvesEachMultiUserTagToItsPlatformAndSuffix(String tags, String expectedPlatform, String expectedSuffix) {
        PlatformTagResolver.Resolution resolution = PlatformTagResolver.resolve(tags);

        assertThat(resolution).isNotNull();
        assertThat(resolution.platform()).isEqualTo(Platform.valueOf(expectedPlatform));
        assertThat(resolution.launchNameSuffix()).isEqualTo(expectedSuffix);
    }

    @Test
    void prefersAndroidWebOverAndroidWhenBothSubstringsPresent() {
        // "multiuser-android-web" contains "multiuser-android"; the more specific rule must win.
        PlatformTagResolver.Resolution resolution = PlatformTagResolver.resolve("@multiuser-android-web and @smoke");

        assertThat(resolution.platform()).isEqualTo(Platform.android);
        assertThat(resolution.launchNameSuffix()).isEqualTo(" - Real User Simulation on Android & Web");
    }

    @ParameterizedTest
    @ValueSource(strings = {"@smoke", "@regression and not @wip", "singleuser-web"})
    void returnsNullWhenNoMultiUserTagMatches(String tags) {
        assertThat(PlatformTagResolver.resolve(tags)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void returnsNullForNullOrEmptyTags(String tags) {
        assertThat(PlatformTagResolver.resolve(tags)).isNull();
    }
}
