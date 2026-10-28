package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CiProvider} — the provider-agnostic CI detection + field mapping that lets
 * {@link RunEnvironment} describe a run regardless of which CI system produced it. Each provider is detected by its
 * own signature environment variable and maps its native variable names to teswiz's common run-provenance fields.
 */
class CiProviderTest {

    @Test
    void gitLabIsDetectedByPipelineIdAndMapsItsNativeVariables() {
        Map<String, String> env = Map.of("GITLAB_CI", "true", "CI_PIPELINE_ID", "1234567", "CI_PIPELINE_URL",
                "https://gitlab/pipe/1234567", "CI_JOB_ID", "8901234", "CI_JOB_NAME", "sup-row-uk", "CI_JOB_URL",
                "https://gitlab/job/8901234", "CI_COMMIT_SHORT_SHA", "af9a330a", "GITLAB_USER_NAME", "Alice",
                "CI_RUNNER_DESCRIPTION", "shared-runner-7");

        CiProvider provider = CiProvider.detect(env);

        assertThat(provider.isCi()).isTrue();
        assertThat(provider.displayName()).isEqualTo("GitLab");
        assertThat(provider.pipelineId(env)).isEqualTo("1234567");
        assertThat(provider.pipelineUrl(env)).isEqualTo("https://gitlab/pipe/1234567");
        assertThat(provider.jobName(env)).isEqualTo("sup-row-uk");
        assertThat(provider.commit(env)).isEqualTo("af9a330a");
        assertThat(provider.triggeredBy(env)).isEqualTo("Alice");
        assertThat(provider.runner(env)).isEqualTo("shared-runner-7");
    }

    @Test
    void gitHubActionsIsDetectedAndMapsItsNativeVariables() {
        Map<String, String> env = Map.of("GITHUB_ACTIONS", "true", "GITHUB_RUN_ID", "556677", "GITHUB_JOB", "build",
                "GITHUB_SERVER_URL", "https://github.com", "GITHUB_REPOSITORY", "znsio/teswiz", "GITHUB_SHA",
                "abcdef1234567890", "GITHUB_ACTOR", "octocat", "RUNNER_NAME", "gh-runner-2");

        CiProvider provider = CiProvider.detect(env);

        assertThat(provider.isCi()).isTrue();
        assertThat(provider.displayName()).isEqualTo("GitHub Actions");
        assertThat(provider.pipelineId(env)).isEqualTo("556677");
        assertThat(provider.pipelineUrl(env))
                .isEqualTo("https://github.com/znsio/teswiz/actions/runs/556677");
        assertThat(provider.jobName(env)).isEqualTo("build");
        assertThat(provider.commit(env)).isEqualTo("abcdef1");
        assertThat(provider.triggeredBy(env)).isEqualTo("octocat");
        assertThat(provider.runner(env)).isEqualTo("gh-runner-2");
    }

    @Test
    void jenkinsIsDetectedAndMapsItsNativeVariables() {
        Map<String, String> env = Map.of("JENKINS_URL", "https://jenkins", "BUILD_NUMBER", "42", "JOB_NAME",
                "teswiz-pipeline", "BUILD_URL", "https://jenkins/job/teswiz/42/", "GIT_COMMIT",
                "0123456789abcdef", "NODE_NAME", "agent-linux-1");

        CiProvider provider = CiProvider.detect(env);

        assertThat(provider.isCi()).isTrue();
        assertThat(provider.displayName()).isEqualTo("Jenkins");
        assertThat(provider.pipelineId(env)).isEqualTo("42");
        assertThat(provider.jobName(env)).isEqualTo("teswiz-pipeline");
        assertThat(provider.jobUrl(env)).isEqualTo("https://jenkins/job/teswiz/42/");
        assertThat(provider.commit(env)).isEqualTo("0123456");
        assertThat(provider.runner(env)).isEqualTo("agent-linux-1");
    }

    @Test
    void circleCiIsDetectedAndMapsItsNativeVariables() {
        Map<String, String> env = Map.of("CIRCLECI", "true", "CIRCLE_WORKFLOW_ID", "wf-9988", "CIRCLE_JOB",
                "unit-tests", "CIRCLE_BUILD_URL", "https://circleci/job/9988", "CIRCLE_SHA1", "fedcba9876543210",
                "CIRCLE_USERNAME", "builder", "CIRCLE_NODE_INDEX", "0");

        CiProvider provider = CiProvider.detect(env);

        assertThat(provider.isCi()).isTrue();
        assertThat(provider.displayName()).isEqualTo("CircleCI");
        assertThat(provider.pipelineId(env)).isEqualTo("wf-9988");
        assertThat(provider.jobName(env)).isEqualTo("unit-tests");
        assertThat(provider.jobUrl(env)).isEqualTo("https://circleci/job/9988");
        assertThat(provider.commit(env)).isEqualTo("fedcba9");
        assertThat(provider.triggeredBy(env)).isEqualTo("builder");
    }

    @Test
    void anEmptyEnvironmentIsLocalAndNotCi() {
        CiProvider provider = CiProvider.detect(Map.of());

        assertThat(provider.isCi()).isFalse();
        assertThat(provider.displayName()).isEqualTo("Local");
        assertThat(provider.pipelineId(Map.of())).isEmpty();
        assertThat(provider.commit(Map.of())).isEmpty();
    }

    @Test
    void commitIsShortenedToSevenCharactersWhenAProviderExposesTheFullSha() {
        Map<String, String> env = Map.of("GITHUB_ACTIONS", "true", "GITHUB_SHA", "1234567890abcdef1234567890abcdef");

        assertThat(CiProvider.detect(env).commit(env)).isEqualTo("1234567");
    }
}
