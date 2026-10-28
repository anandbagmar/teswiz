package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests proving {@link RunEnvironment} is CI-provider-agnostic: it resolves the same run-provenance fields from a
 * GitHub Actions run, a Jenkins run, a local run, etc. — not just GitLab. The provider is detected from the
 * environment and its native variables are mapped onto teswiz's common fields.
 */
class RunEnvironmentGenericCiTest {

    @Test
    void gitHubActionsRunIsMarkedCiAndCarriesPipelineJobAndCommit() {
        Map<String, String> env = new HashMap<>();
        env.put("GITHUB_ACTIONS", "true");
        env.put("GITHUB_RUN_ID", "556677");
        env.put("GITHUB_JOB", "unit-tests");
        env.put("GITHUB_SERVER_URL", "https://github.com");
        env.put("GITHUB_REPOSITORY", "znsio/teswiz");
        env.put("GITHUB_SHA", "abcdef1234567890");
        env.put("GITHUB_ACTOR", "octocat");

        RunEnvironment runEnvironment = RunEnvironment.resolve(env, "main", () -> "ignored", () -> "ignored");

        assertThat(runEnvironment.isCi()).isTrue();
        assertThat(runEnvironment.ciProvider()).isEqualTo("GitHub Actions");
        assertThat(runEnvironment.pipelineId()).isEqualTo("556677");
        assertThat(runEnvironment.jobName()).isEqualTo("unit-tests");
        assertThat(runEnvironment.commit()).isEqualTo("abcdef1");
        assertThat(runEnvironment.triggeredBy()).isEqualTo("octocat");
        assertThat(runEnvironment.branch()).isEqualTo("main");
        assertThat(runEnvironment.host()).isEmpty();
        assertThat(runEnvironment.user()).isEmpty();
    }

    @Test
    void jenkinsRunIsMarkedCiAndCarriesBuildNumberJobAndCommit() {
        Map<String, String> env = new HashMap<>();
        env.put("JENKINS_URL", "https://jenkins");
        env.put("BUILD_NUMBER", "42");
        env.put("JOB_NAME", "teswiz-pipeline");
        env.put("BUILD_URL", "https://jenkins/job/teswiz/42/");
        env.put("GIT_COMMIT", "0123456789abcdef");

        RunEnvironment runEnvironment = RunEnvironment.resolve(env, "release", () -> "ignored", () -> "ignored");

        assertThat(runEnvironment.isCi()).isTrue();
        assertThat(runEnvironment.ciProvider()).isEqualTo("Jenkins");
        assertThat(runEnvironment.pipelineId()).isEqualTo("42");
        assertThat(runEnvironment.jobName()).isEqualTo("teswiz-pipeline");
        assertThat(runEnvironment.jobUrl()).isEqualTo("https://jenkins/job/teswiz/42/");
    }

    @Test
    void aLocalRunReportsTheLocalProviderAndCarriesHostAndUser() {
        RunEnvironment runEnvironment = RunEnvironment.resolve(Map.of("INTEGRATION", "sup_cleo"), "my-branch",
                () -> "MacBook.local", () -> "Anand.Bagmar");

        assertThat(runEnvironment.isCi()).isFalse();
        assertThat(runEnvironment.ciProvider()).isEqualTo("Local");
        assertThat(runEnvironment.host()).isEqualTo("MacBook.local");
        assertThat(runEnvironment.user()).isEqualTo("Anand.Bagmar");
    }
}
