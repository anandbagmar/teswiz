package com.znsio.teswiz.reporting.notify;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Provider-agnostic CI detection + field mapping — the single source of truth for "which CI is this and what does it
 * expose" across teswiz. Each supported CI system is detected by its own signature environment variable(s), and
 * exposes the run's provenance (pipeline / job identifiers and URLs, the short commit, who triggered it, and the
 * executing runner) by reading that provider's <em>native</em> variable names. teswiz therefore describes a run the
 * same way no matter which CI produced it, and a provider teswiz does not recognise simply resolves as {@link #LOCAL}
 * — the fields it cannot supply come back empty rather than breaking.
 * <p>
 * Two consumers share this one definition so the provider list never drifts: the notification footer (via
 * {@link #detect(Map)} + {@link RunEnvironment}, which uses the field mappings and the human-readable
 * {@link #displayName()}) and run telemetry (via {@link #telemetryLabel(Map, boolean)}, which uses the stable
 * snake_case {@link #telemetryLabel()} and adds a {@code RUN_IN_CI}-driven generic-CI fallback). Adding a provider
 * here updates both.
 * <p>
 * This replaces the earlier GitLab-only assumption (teswiz used GitLab's {@code CI_*} names as if they were universal).
 * Detection order matters only in that each provider's signature variable is distinct; the first match wins.
 */
public enum CiProvider {

    GITLAB("GitLab", "gitlab", false, new String[] {"CI_PIPELINE_ID", "GITLAB_CI"}, env -> env.get("CI_PIPELINE_ID"),
            env -> env.get("CI_PIPELINE_URL"), env -> env.get("CI_JOB_ID"), env -> env.get("CI_JOB_NAME"),
            env -> env.get("CI_JOB_URL"), env -> env.get("CI_COMMIT_SHORT_SHA"), env -> env.get("GITLAB_USER_NAME"),
            env -> env.get("CI_RUNNER_DESCRIPTION")),

    GITHUB_ACTIONS("GitHub Actions", "github_actions", true, new String[] {"GITHUB_ACTIONS"},
            env -> env.get("GITHUB_RUN_ID"), CiProvider::gitHubActionsRunUrl, env -> env.get("GITHUB_RUN_NUMBER"),
            env -> env.get("GITHUB_JOB"), CiProvider::gitHubActionsRunUrl, env -> env.get("GITHUB_SHA"),
            env -> env.get("GITHUB_ACTOR"), env -> env.get("RUNNER_NAME")),

    JENKINS("Jenkins", "jenkins", true, new String[] {"JENKINS_URL"}, env -> env.get("BUILD_NUMBER"),
            env -> env.get("BUILD_URL"), env -> env.get("BUILD_NUMBER"), env -> env.get("JOB_NAME"),
            env -> env.get("BUILD_URL"), env -> env.get("GIT_COMMIT"), env -> env.get("BUILD_USER"),
            env -> env.get("NODE_NAME")),

    CIRCLECI("CircleCI", "circleci", true, new String[] {"CIRCLECI"}, env -> env.get("CIRCLE_WORKFLOW_ID"),
            env -> env.get("CIRCLE_BUILD_URL"), env -> env.get("CIRCLE_BUILD_NUM"), env -> env.get("CIRCLE_JOB"),
            env -> env.get("CIRCLE_BUILD_URL"), env -> env.get("CIRCLE_SHA1"), env -> env.get("CIRCLE_USERNAME"),
            env -> env.get("CIRCLE_NODE_INDEX")),

    BITBUCKET("Bitbucket Pipelines", "bitbucket", true, new String[] {"BITBUCKET_BUILD_NUMBER", "BITBUCKET_COMMIT"},
            env -> env.get("BITBUCKET_BUILD_NUMBER"), env -> env.get("BITBUCKET_GIT_HTTP_ORIGIN"),
            env -> env.get("BITBUCKET_BUILD_NUMBER"), env -> env.get("BITBUCKET_PIPELINE_UUID"),
            env -> env.get("BITBUCKET_GIT_HTTP_ORIGIN"), env -> env.get("BITBUCKET_COMMIT"),
            env -> env.get("BITBUCKET_STEP_TRIGGERER_UUID"), env -> ""),

    AZURE_PIPELINES("Azure Pipelines", "azure_devops", true, new String[] {"TF_BUILD"},
            env -> env.get("BUILD_BUILDID"), env -> env.get("BUILD_BUILDURI"), env -> env.get("BUILD_BUILDID"),
            env -> env.get("AGENT_JOBNAME"), env -> env.get("BUILD_BUILDURI"), env -> env.get("BUILD_SOURCEVERSION"),
            env -> env.get("BUILD_REQUESTEDFOR"), env -> env.get("AGENT_NAME")),

    LOCAL("Local", "local", false, new String[] {}, env -> "", env -> "", env -> "", env -> "", env -> "", env -> "",
            env -> "", env -> "");

    private static final int SHORT_SHA_LENGTH = 7;
    private static final String GENERIC_CI_LABEL = "ci_generic";

    private final String displayName;
    private final String telemetryLabel;
    private final boolean shortensCommit;
    private final String[] signatureVariables;
    private final Function<Map<String, String>, String> pipelineId;
    private final Function<Map<String, String>, String> pipelineUrl;
    private final Function<Map<String, String>, String> jobId;
    private final Function<Map<String, String>, String> jobName;
    private final Function<Map<String, String>, String> jobUrl;
    private final Function<Map<String, String>, String> commit;
    private final Function<Map<String, String>, String> triggeredBy;
    private final Function<Map<String, String>, String> runner;

    CiProvider(String displayName, String telemetryLabel, boolean shortensCommit, String[] signatureVariables,
            Function<Map<String, String>, String> pipelineId, Function<Map<String, String>, String> pipelineUrl,
            Function<Map<String, String>, String> jobId, Function<Map<String, String>, String> jobName,
            Function<Map<String, String>, String> jobUrl, Function<Map<String, String>, String> commit,
            Function<Map<String, String>, String> triggeredBy, Function<Map<String, String>, String> runner) {
        this.displayName = displayName;
        this.telemetryLabel = telemetryLabel;
        this.shortensCommit = shortensCommit;
        this.signatureVariables = signatureVariables;
        this.pipelineId = pipelineId;
        this.pipelineUrl = pipelineUrl;
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobUrl = jobUrl;
        this.commit = commit;
        this.triggeredBy = triggeredBy;
        this.runner = runner;
    }

    /**
     * Detects the CI provider from the environment by matching each provider's signature variable(s) in declaration
     * order; returns {@link #LOCAL} when none match. This is the env-signature detection the notification footer uses
     * (it does not consider the {@code RUN_IN_CI} config flag — that is a telemetry-only fallback, see
     * {@link #telemetryLabel(Map, boolean)}).
     *
     * @param environment the process environment to inspect
     * @return the detected provider (never {@code null})
     */
    static CiProvider detect(Map<String, String> environment) {
        for (CiProvider provider : values()) {
            if (provider.matches(environment)) {
                return provider;
            }
        }
        return LOCAL;
    }

    /**
     * Resolves the stable snake_case telemetry label for a run: the detected provider's {@link #telemetryLabel()},
     * else {@code "ci_generic"} when the environment is unrecognised but the run is flagged as CI, else
     * {@code "local"}. This is the single detection path telemetry delegates to, so the provider list is defined once.
     *
     * @param environment the process environment to inspect
     * @param runInCi      whether teswiz's {@code RUN_IN_CI} config flag is set (the generic-CI fallback)
     * @return the telemetry label
     */
    public static String telemetryLabel(Map<String, String> environment, boolean runInCi) {
        CiProvider provider = detect(environment);
        if (provider.isCi()) {
            return provider.telemetryLabel();
        }
        return runInCi ? GENERIC_CI_LABEL : LOCAL.telemetryLabel();
    }

    private boolean matches(Map<String, String> environment) {
        for (String signatureVariable : signatureVariables) {
            if (!value(environment, signatureVariable).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} for every provider except {@link #LOCAL}
     */
    boolean isCi() {
        return this != LOCAL;
    }

    /**
     * @return the human-readable provider name used in the notification footer
     */
    String displayName() {
        return displayName;
    }

    /**
     * @return the stable snake_case label used in the telemetry event (e.g. {@code github_actions})
     */
    public String telemetryLabel() {
        return telemetryLabel;
    }

    String pipelineId(Map<String, String> environment) {
        return trimmed(pipelineId, environment);
    }

    String pipelineUrl(Map<String, String> environment) {
        return trimmed(pipelineUrl, environment);
    }

    String jobId(Map<String, String> environment) {
        return trimmed(jobId, environment);
    }

    String jobName(Map<String, String> environment) {
        return trimmed(jobName, environment);
    }

    String jobUrl(Map<String, String> environment) {
        return trimmed(jobUrl, environment);
    }

    /**
     * Returns the commit for this run. Providers that expose a full SHA (GitHub, Jenkins, CircleCI, …) are shortened
     * to 7 chars; GitLab already exposes a short SHA ({@code CI_COMMIT_SHORT_SHA}) and is returned verbatim.
     *
     * @param environment the process environment
     * @return the (short) commit, or an empty string
     */
    String commit(Map<String, String> environment) {
        String sha = trimmed(commit, environment);
        if (shortensCommit && sha.length() > SHORT_SHA_LENGTH) {
            return sha.substring(0, SHORT_SHA_LENGTH);
        }
        return sha;
    }

    String triggeredBy(Map<String, String> environment) {
        return trimmed(triggeredBy, environment);
    }

    String runner(Map<String, String> environment) {
        return trimmed(runner, environment);
    }

    private String trimmed(Function<Map<String, String>, String> mapper, Map<String, String> environment) {
        String raw = mapper.apply(environment);
        return raw == null ? "" : raw.trim();
    }

    private static String gitHubActionsRunUrl(Map<String, String> environment) {
        String server = value(environment, "GITHUB_SERVER_URL");
        String repository = value(environment, "GITHUB_REPOSITORY");
        String runId = value(environment, "GITHUB_RUN_ID");
        if (server.isEmpty() || repository.isEmpty() || runId.isEmpty()) {
            return "";
        }
        return String.join("/", List.of(server, repository, "actions", "runs", runId));
    }

    private static String value(Map<String, String> environment, String key) {
        String raw = environment.get(key);
        return raw == null ? "" : raw.trim();
    }
}
