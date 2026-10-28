package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Pins the telemetry labels {@link CiProvider} exposes so it can be the single source of truth for CI detection —
 * {@code RunTelemetry} delegates to {@link CiProvider#telemetryLabel(Map, boolean)} instead of carrying its own copy
 * of the detection logic. These labels are the stable snake_case strings the telemetry event has always emitted and
 * must not drift: {@code github_actions}, {@code jenkins}, {@code gitlab}, {@code azure_devops}, {@code circleci},
 * {@code bitbucket}, with {@code ci_generic} and {@code local} as the two fallbacks.
 */
class CiProviderTelemetryLabelTest {

    @Test
    void eachProviderExposesItsStableSnakeCaseTelemetryLabel() {
        assertThat(CiProvider.GITHUB_ACTIONS.telemetryLabel()).isEqualTo("github_actions");
        assertThat(CiProvider.JENKINS.telemetryLabel()).isEqualTo("jenkins");
        assertThat(CiProvider.GITLAB.telemetryLabel()).isEqualTo("gitlab");
        assertThat(CiProvider.AZURE_PIPELINES.telemetryLabel()).isEqualTo("azure_devops");
        assertThat(CiProvider.CIRCLECI.telemetryLabel()).isEqualTo("circleci");
        assertThat(CiProvider.BITBUCKET.telemetryLabel()).isEqualTo("bitbucket");
        assertThat(CiProvider.LOCAL.telemetryLabel()).isEqualTo("local");
    }

    @Test
    void telemetryDetectionMatchesTheOriginalPrecedenceWhenAProviderIsDetected() {
        assertThat(CiProvider.telemetryLabel(Map.of("GITHUB_ACTIONS", "true"), false)).isEqualTo("github_actions");
        assertThat(CiProvider.telemetryLabel(Map.of("JENKINS_URL", "https://jenkins"), false)).isEqualTo("jenkins");
        assertThat(CiProvider.telemetryLabel(Map.of("GITLAB_CI", "true", "CI_PIPELINE_ID", "1"), false))
                .isEqualTo("gitlab");
        assertThat(CiProvider.telemetryLabel(Map.of("TF_BUILD", "True"), false)).isEqualTo("azure_devops");
        assertThat(CiProvider.telemetryLabel(Map.of("CIRCLECI", "true"), false)).isEqualTo("circleci");
    }

    @Test
    void bitbucketIsRecognisedForTelemetryByItsCommitVariableToo() {
        // RunTelemetry historically keyed Bitbucket off BITBUCKET_COMMIT; keep that recognised.
        assertThat(CiProvider.telemetryLabel(Map.of("BITBUCKET_COMMIT", "abc123"), false)).isEqualTo("bitbucket");
    }

    @Test
    void anUnrecognisedEnvironmentWithRunInCiFlagFallsBackToGenericCi() {
        assertThat(CiProvider.telemetryLabel(Map.of(), true)).isEqualTo("ci_generic");
    }

    @Test
    void anUnrecognisedEnvironmentWithoutRunInCiFlagIsLocal() {
        assertThat(CiProvider.telemetryLabel(Map.of(), false)).isEqualTo("local");
    }
}
