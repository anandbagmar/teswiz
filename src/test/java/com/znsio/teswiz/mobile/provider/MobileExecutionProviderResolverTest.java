package com.znsio.teswiz.mobile.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MobileExecutionProviderResolverTest {

    private final MobileExecutionProviderResolver resolver = new MobileExecutionProviderResolver();

    @ParameterizedTest
    @CsvSource({
            "browserstack, BrowserStackMobileExecutionProvider",
            "BrowserStack, BrowserStackMobileExecutionProvider",
            "lambdatest, LambdaTestMobileExecutionProvider",
            "headspin, HeadSpinMobileExecutionProvider",
            "pcloudy, PCloudyMobileExecutionProvider",
            "pCloudy, PCloudyMobileExecutionProvider",
            "local, LocalMobileExecutionProvider"
    })
    void resolvesKnownProviderNamesCaseInsensitively(String providerName, String expectedSimpleClassName) {
        assertThat(resolver.resolve(providerName).getClass().getSimpleName())
                .isEqualTo(expectedSimpleClassName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "saucelabs", "not-set"})
    void resolvesUnknownProviderNameToLocal(String providerName) {
        assertThat(resolver.resolve(providerName)).isInstanceOf(LocalMobileExecutionProvider.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void resolvesBlankProviderNameToLocal(String providerName) {
        assertThat(resolver.resolve(providerName)).isInstanceOf(LocalMobileExecutionProvider.class);
    }

    @Test
    void resolvesWhitespaceProviderNameToLocal() {
        assertThat(resolver.resolve("   ")).isInstanceOf(LocalMobileExecutionProvider.class);
    }
}
