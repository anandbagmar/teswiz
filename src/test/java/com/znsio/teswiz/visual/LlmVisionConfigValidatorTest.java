package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.exceptions.InvalidTestDataException;
import com.znsio.teswiz.runner.Setup;

/**
 * Covers the fail-fast structural validation branches of {@link LlmVisionConfigValidator} that throw
 * before any network health-check (so no live endpoint is required). The fully-valid path is not
 * exercised here because it proceeds to a real health-check HTTP call.
 */
class LlmVisionConfigValidatorTest {

    @BeforeEach
    void loadBaseConfig() {
        Setup.load("configs/teswiz/teswiz_config.properties.template");
        Setup.loadAndUpdateConfigParameters("configs/teswiz/teswiz_config.properties.template");
        clearLlmSystemProps();
    }

    @AfterEach
    void cleanup() {
        clearLlmSystemProps();
    }

    private void clearLlmSystemProps() {
        System.clearProperty(Setup.LLM_VISION_FALLBACK_ENABLED);
        System.clearProperty(Setup.LLM_VISION_PROVIDER);
        System.clearProperty(Setup.LLM_VISION_ENDPOINT);
        System.clearProperty(Setup.LLM_VISION_MODEL);
        System.clearProperty(Setup.LLM_VISION_API_KEY_ENV);
    }

    @Test
    void disabledFallbackSkipsValidationEntirely() {
        Setup.addBooleanValueToConfigs(Setup.LLM_VISION_FALLBACK_ENABLED, false);
        assertThatCode(LlmVisionConfigValidator::validateIfEnabled).doesNotThrowAnyException();
    }

    @Test
    void unsupportedProviderFailsFast() {
        enableWith("totally-made-up", "https://api.example.com/v1/chat/completions");
        assertThatThrownBy(LlmVisionConfigValidator::validateIfEnabled)
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("Unsupported LLM_VISION_PROVIDER");
    }

    @Test
    void blankEndpointFailsFast() {
        enableWith("openai", "");
        assertThatThrownBy(LlmVisionConfigValidator::validateIfEnabled)
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("LLM_VISION_ENDPOINT is mandatory");
    }

    @Test
    void malformedEndpointUrlFailsFast() {
        enableWith("openai", "not-a-url");
        assertThatThrownBy(LlmVisionConfigValidator::validateIfEnabled)
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("not a well-formed");
    }

    @Test
    void modelRequiredProviderWithBlankModelFailsFast() {
        // groq requires an explicit model id
        enableWith("groq", "https://api.groq.com/openai/v1/chat/completions");
        Setup.addToConfigs(Setup.LLM_VISION_MODEL, "");
        assertThatThrownBy(LlmVisionConfigValidator::validateIfEnabled)
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("LLM_VISION_MODEL is mandatory");
    }

    @Test
    void missingApiKeyEnvVarFailsFast() {
        enableWith("openai", "https://api.openai.com/v1/chat/completions");
        // point the key env-var name at something guaranteed unset
        Setup.addToConfigs(Setup.LLM_VISION_API_KEY_ENV, "TESWIZ_DEFINITELY_UNSET_KEY_" + System.nanoTime());
        assertThatThrownBy(LlmVisionConfigValidator::validateIfEnabled)
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("is not set or is empty");
    }

    private void enableWith(String provider, String endpoint) {
        Setup.addBooleanValueToConfigs(Setup.LLM_VISION_FALLBACK_ENABLED, true);
        Setup.addToConfigs(Setup.LLM_VISION_PROVIDER, provider);
        Setup.addToConfigs(Setup.LLM_VISION_ENDPOINT, endpoint);
    }
}
