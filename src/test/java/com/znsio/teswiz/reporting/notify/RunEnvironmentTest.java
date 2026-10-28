package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RunEnvironment}.
 * <p>
 * The provenance is resolved from an injected environment map, an already-resolved branch, and host/user suppliers, so
 * it is deterministic offline. A GitLab run (CI_PIPELINE_ID present) carries pipeline/job/commit/runner; a local run
 * carries host + user. Both carry the platform/environment prefix inputs: these prefer the injected environment map and
 * only fall back to teswiz's resolved configuration when the map has no value.
 */
class RunEnvironmentTest {

    @Test
    void ciRunResolvesPipelineJobAndIsMarkedCi() {
        RunEnvironment env = RunEnvironment.resolve(
                Map.of("CI_PIPELINE_ID", "1234567", "CI_PIPELINE_URL", "https://gitlab/pipe/1234567", "CI_JOB_ID",
                        "8901234", "CI_JOB_NAME", "sup_pragmaticplay-row-uk", "CI_JOB_URL",
                        "https://gitlab/job/8901234", "CI_COMMIT_SHORT_SHA", "af9a330a", "INTEGRATION",
                        "sup_pragmaticplay"),
                "F_GAMC-19572_notifications", () -> "ignored-on-ci", () -> "ignored-on-ci");

        assertThat(env.isCi()).isTrue();
        assertThat(env.pipelineId()).isEqualTo("1234567");
        assertThat(env.jobName()).isEqualTo("sup_pragmaticplay-row-uk");
        assertThat(env.commit()).isEqualTo("af9a330a");
        assertThat(env.branch()).isEqualTo("F_GAMC-19572_notifications");
        // The host/user suppliers are not consulted on a CI run.
        assertThat(env.host()).isEmpty();
        assertThat(env.user()).isEmpty();
    }

    @Test
    void localRunResolvesHostAndUserAndIsNotCi() {
        RunEnvironment env = RunEnvironment.resolve(Map.of("INTEGRATION", "sup_cleo"), "my-feature-branch",
                () -> "Anands-MacBook-Pro.local", () -> "Anand.Bagmar");

        assertThat(env.isCi()).isFalse();
        assertThat(env.host()).isEqualTo("Anands-MacBook-Pro.local");
        assertThat(env.user()).isEqualTo("Anand.Bagmar");
        assertThat(env.branch()).isEqualTo("my-feature-branch");
    }

    @Test
    void platformAndEnvironmentFromTheEnvironmentMapAreUsedWhenPresent() {
        RunEnvironment env = RunEnvironment.resolve(Map.of("PLATFORM", "web", "TARGET_ENVIRONMENT", "STAGE"), "branch",
                () -> "host", () -> "user");

        assertThat(env.platform()).isEqualTo("web");
        assertThat(env.environment()).isEqualTo("STAGE");
    }

    @Test
    void platformAndEnvironmentFallBackToResolvedConfigAndDegradeToEmptyWhenUnavailable() {
        // The map carries neither key, so resolve() reads teswiz's resolved configuration via Setup. In this offline
        // unit test Setup is not initialised, so that read fails and degrades to an empty string rather than throwing.
        RunEnvironment env = RunEnvironment.resolve(Map.of("INTEGRATION", "sup_cleo"), "branch", () -> "host",
                () -> "user");

        assertThat(env.platform()).isEmpty();
        assertThat(env.environment()).isEmpty();
    }

    @Test
    void aSupplierExceptionFromHostResolutionDegradesToEmpty() {
        RunEnvironment env = RunEnvironment.resolve(Map.of(), "branch", () -> {
            throw new RuntimeException("no host");
        }, () -> "user");

        assertThat(env.host()).isEmpty();
        assertThat(env.user()).isEqualTo("user");
    }
}
