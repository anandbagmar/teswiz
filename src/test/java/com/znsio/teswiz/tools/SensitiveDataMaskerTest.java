package com.znsio.teswiz.tools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {

    @AfterEach
    void resetMaskerState() {
        SensitiveDataMasker.setShowSensitiveData(false);
        SensitiveDataMasker.resetSensitiveKeysToDefault();
    }

    @Test
    void mask_shouldMaskUrlCredentials() {
        String input = "https://myuser:mypass@example.com/path";

        assertThat(SensitiveDataMasker.mask(input)).isEqualTo("https://***:***@example.com/path");
    }

    @Test
    void mask_shouldMaskCurlUserCredentials() {
        String input = "curl -u 'myuser:mypass' https://example.com";

        assertThat(SensitiveDataMasker.mask(input)).isEqualTo("curl -u '***:***' https://example.com");
    }

    @Test
    void mask_shouldMaskAuthorizationBearerToken() {
        String input = "Authorization: Bearer abc.def.ghi";

        // The bearer token is masked first; the generic "authorization" key/value pass then
        // also matches the already-masked "Authorization: Bearer" prefix.
        assertThat(SensitiveDataMasker.mask(input)).isEqualTo("Authorization=*** ***");
    }

    @Test
    void mask_shouldMaskDefaultSensitiveJsonKeys() {
        String input = "{\"password\": \"p@ss\", \"username\": \"bob\"}";

        assertThat(SensitiveDataMasker.mask(input)).isEqualTo("{\"password\": \"***\", \"username\": \"***\"}");
    }

    @Test
    void mask_shouldNotMaskNonSensitiveJsonKeysByDefault() {
        String input = "{\"sessionId\": \"abc123\"}";

        assertThat(SensitiveDataMasker.mask(input)).isEqualTo("{\"sessionId\": \"abc123\"}");
    }

    @Test
    void mask_shouldReturnOriginalValue_whenShowSensitiveDataIsTrue() {
        SensitiveDataMasker.setShowSensitiveData(true);
        String input = "{\"password\": \"p@ss\"}";

        assertThat(SensitiveDataMasker.mask(input)).isEqualTo(input);
    }

    @Test
    void mask_shouldReturnNull_whenValueIsNull() {
        assertThat(SensitiveDataMasker.mask(null)).isNull();
    }

    @Test
    void mask_shouldReturnBlankValue_whenValueIsBlank() {
        assertThat(SensitiveDataMasker.mask("   ")).isEqualTo("   ");
    }

    @Test
    void maskSecret_shouldMaskNonBlankValue() {
        assertThat(SensitiveDataMasker.maskSecret("some-secret-key")).isEqualTo("***");
    }

    @Test
    void maskSecret_shouldReturnNull_whenValueIsNull() {
        assertThat(SensitiveDataMasker.maskSecret(null)).isNull();
    }

    @Test
    void maskSecret_shouldReturnOriginalValue_whenShowSensitiveDataIsTrue() {
        SensitiveDataMasker.setShowSensitiveData(true);

        assertThat(SensitiveDataMasker.maskSecret("some-secret-key")).isEqualTo("some-secret-key");
    }

    @Test
    void configureSensitiveKeys_withAdditionalKeys_shouldMergeWithDefaults() {
        String input = "{\"password\": \"p@ss\", \"sessionId\": \"abc123\"}";

        SensitiveDataMasker.configureSensitiveKeys(List.of("sessionId"), List.of());

        assertThat(SensitiveDataMasker.mask(input))
                .isEqualTo("{\"password\": \"***\", \"sessionId\": \"***\"}");
    }

    @Test
    void configureSensitiveKeys_withOverrideKeys_shouldReplaceDefaultsEntirely() {
        String input = "{\"password\": \"p@ss\", \"sessionId\": \"abc123\"}";

        SensitiveDataMasker.configureSensitiveKeys(List.of(), List.of("sessionId"));

        assertThat(SensitiveDataMasker.mask(input))
                .isEqualTo("{\"password\": \"p@ss\", \"sessionId\": \"***\"}");
    }

    @Test
    void configureSensitiveKeys_withOverrideKeys_shouldTakePrecedenceOverAdditionalKeys() {
        String input = "{\"password\": \"p@ss\", \"sessionId\": \"abc123\", \"token\": \"tok\"}";

        SensitiveDataMasker.configureSensitiveKeys(List.of("token"), List.of("sessionId"));

        assertThat(SensitiveDataMasker.mask(input))
                .isEqualTo("{\"password\": \"p@ss\", \"sessionId\": \"***\", \"token\": \"tok\"}");
    }

    @Test
    void configureSensitiveKeys_shouldTreatKeysAsLiteralNotRegex() {
        String input = "{\"a.b\": \"shouldMatchLiteralDot\", \"axb\": \"shouldNotMatchAsRegexWildcard\"}";

        SensitiveDataMasker.configureSensitiveKeys(List.of("a.b"), List.of());

        assertThat(SensitiveDataMasker.mask(input))
                .isEqualTo("{\"a.b\": \"***\", \"axb\": \"shouldNotMatchAsRegexWildcard\"}");
    }

    @Test
    void resetSensitiveKeysToDefault_shouldRestoreDefaultKeyList() {
        String input = "{\"password\": \"p@ss\", \"sessionId\": \"abc123\"}";
        SensitiveDataMasker.configureSensitiveKeys(List.of(), List.of("sessionId"));

        SensitiveDataMasker.resetSensitiveKeysToDefault();

        assertThat(SensitiveDataMasker.mask(input))
                .isEqualTo("{\"password\": \"***\", \"sessionId\": \"abc123\"}");
    }

    @Test
    void mask_shouldMaskTextFormSensitiveKeyValue() {
        String input = "password=p@ss token=tok123";

        assertThat(SensitiveDataMasker.mask(input)).isEqualTo("password=*** token=***");
    }
}
