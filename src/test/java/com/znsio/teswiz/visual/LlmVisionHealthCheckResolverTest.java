package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class LlmVisionHealthCheckResolverTest {

    private static final LlmVisionHealthCheckProbe DERIVED =
            new LlmVisionHealthCheckProbe("https://derived.example/v1/models", Map.of("api-key", "secret"));

    @Test
    void blankOverride_usesProviderDerivedProbe() {
        Optional<LlmVisionHealthCheckProbe> resolved =
                LlmVisionHealthCheckResolver.resolve("", Optional.of(DERIVED), Map.of("api-key", "secret"));

        assertThat(resolved).contains(DERIVED);
    }

    @Test
    void blankOverride_withNoDerivedProbe_isStructuralOnly() {
        Optional<LlmVisionHealthCheckProbe> resolved =
                LlmVisionHealthCheckResolver.resolve("   ", Optional.empty(), Map.of("x-api-key", "secret"));

        assertThat(resolved).isEmpty();
    }

    @Test
    void explicitUrlOverride_winsOverDerivedProbeAndReusesProviderHeaders() {
        Optional<LlmVisionHealthCheckProbe> resolved = LlmVisionHealthCheckResolver.resolve(
                "https://override.example/models", Optional.of(DERIVED), Map.of("x-api-key", "k"));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().url()).isEqualTo("https://override.example/models");
        assertThat(resolved.get().headers()).containsEntry("x-api-key", "k");
    }

    @Test
    void explicitUrlOverride_winsEvenWhenProviderHasNoDerivedProbe() {
        Optional<LlmVisionHealthCheckProbe> resolved = LlmVisionHealthCheckResolver.resolve(
                "https://override.example/models", Optional.empty(), Map.of("Authorization", "Bearer k"));

        assertThat(resolved).isPresent();
        assertThat(resolved.get().url()).isEqualTo("https://override.example/models");
    }

    @Test
    void optOutValues_skipTheLiveProbeRegardlessOfDerivedProbe() {
        for (String optOut : new String[] {"none", "NONE", "off", "Off", "false"}) {
            assertThat(LlmVisionHealthCheckResolver.resolve(optOut, Optional.of(DERIVED), Map.of("api-key", "secret")))
                    .as("opt-out value '%s'", optOut)
                    .isEmpty();
        }
    }

    @Test
    void malformedOverrideUrl_isRejected() {
        assertThat(LlmVisionHealthCheckResolver.isOptOut("none")).isTrue();
        assertThat(LlmVisionHealthCheckResolver.isWellFormedHttpUrl("not-a-url")).isFalse();
        assertThat(LlmVisionHealthCheckResolver.isWellFormedHttpUrl("https://ok.example/models")).isTrue();
    }
}
