package com.znsio.teswiz.web.provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class WebExecutionProviderResolverTest {

    private final WebExecutionProviderResolver resolver = new WebExecutionProviderResolver();

    @ParameterizedTest
    @CsvSource({
            "browserstack, BrowserStackWebExecutionProvider",
            "BrowserStack, BrowserStackWebExecutionProvider",
            "lambdatest, LambdaTestWebExecutionProvider",
            "headspin, HeadSpinWebExecutionProvider",
            "local, LocalWebExecutionProvider"
    })
    void resolvesKnownProviderNamesCaseInsensitively(String providerName, String expectedSimpleClassName) {
        assertThat(resolver.resolve(providerName).getClass().getSimpleName())
                .isEqualTo(expectedSimpleClassName);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "pcloudy", "not-set"})
    void resolvesUnknownProviderNameToLocal(String providerName) {
        assertThat(resolver.resolve(providerName)).isInstanceOf(LocalWebExecutionProvider.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void resolvesBlankProviderNameToLocal(String providerName) {
        assertThat(resolver.resolve(providerName)).isInstanceOf(LocalWebExecutionProvider.class);
    }

    @Test
    void resolvesWhitespaceProviderNameToLocal() {
        assertThat(resolver.resolve("   ")).isInstanceOf(LocalWebExecutionProvider.class);
    }
}
