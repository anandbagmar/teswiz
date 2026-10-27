package com.znsio.teswiz.reporting.notify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The provenance of a single run, captured for the notification subject and footer so a recipient can trace a result
 * back to where it ran. A GitLab CI run carries the pipeline and job identifiers + URLs, the branch, the short commit,
 * who triggered it and which runner executed it; a local run carries the host machine name and the logged-in user. Both
 * carry the platform (web/api) and target environment (for the subject prefix) and a reconstructed, copy-pasteable
 * {@code reproduce} command built from the resolved run inputs ({@code INTEGRATION} / {@code TAG} / {@code CONFIG} /
 * {@code TARGET_ENVIRONMENT}) — faithful and reproducible, though not the literal keystrokes (the original argv is not
 * recoverable in the separate notify JVM).
 * <p>
 * {@link #resolve} reads everything from an injected environment map, the already-resolved branch, and a host-name and
 * user-name supplier, so the resolution is deterministic and unit-testable offline; the reporter passes
 * {@code System.getenv()}, the branch it already resolved (CI ref else git), and the OS host / user suppliers. The
 * branch is passed in rather than re-derived here so branch resolution lives in exactly one place.
 */
public record RunEnvironment(boolean isCi, String platform, String environment, String pipelineId, String pipelineUrl,
        String jobId, String jobName, String jobUrl, String branch, String commit, String triggeredBy, String runner,
        String host, String user, String reproduceCommand) {

    private static final String CI_PIPELINE_ID = "CI_PIPELINE_ID";
    private static final String CI_PIPELINE_URL = "CI_PIPELINE_URL";
    private static final String CI_JOB_ID = "CI_JOB_ID";
    private static final String CI_JOB_NAME = "CI_JOB_NAME";
    private static final String CI_JOB_URL = "CI_JOB_URL";
    private static final String CI_COMMIT_SHORT_SHA = "CI_COMMIT_SHORT_SHA";
    private static final String GITLAB_USER_NAME = "GITLAB_USER_NAME";
    private static final String CI_RUNNER_DESCRIPTION = "CI_RUNNER_DESCRIPTION";
    private static final String PLATFORM = "PLATFORM";
    private static final String TARGET_ENVIRONMENT = "TARGET_ENVIRONMENT";

    private static final List<String> REPRODUCE_KEYS = List.of("INTEGRATION", "TAG", "CONFIG", "TARGET_ENVIRONMENT");
    private static final String GRADLE_RUN = "./gradlew run";

    /**
     * Resolves the run provenance from the environment, the already-resolved branch, and the host / user suppliers.
     *
     * @param environment  the process environment (CI vars + run inputs); the reporter passes {@code System.getenv()}
     * @param branch       the branch the reporter already resolved (CI ref else git); reused here, not re-derived
     * @param hostSupplier the OS host-name supplier (used only for a local run)
     * @param userSupplier the logged-in user-name supplier (used only for a local run)
     * @return the resolved, immutable run environment
     */
    public static RunEnvironment resolve(Map<String, String> environment, String branch, Supplier<String> hostSupplier,
            Supplier<String> userSupplier) {
        boolean ci = !value(environment, CI_PIPELINE_ID).isEmpty();
        return new RunEnvironment(ci, value(environment, PLATFORM), value(environment, TARGET_ENVIRONMENT),
                value(environment, CI_PIPELINE_ID), value(environment, CI_PIPELINE_URL), value(environment, CI_JOB_ID),
                value(environment, CI_JOB_NAME), value(environment, CI_JOB_URL), branch == null ? "" : branch.trim(),
                value(environment, CI_COMMIT_SHORT_SHA), value(environment, GITLAB_USER_NAME),
                value(environment, CI_RUNNER_DESCRIPTION), ci ? "" : safeGet(hostSupplier),
                ci ? "" : safeGet(userSupplier), reproduceCommand(environment));
    }

    private static String reproduceCommand(Map<String, String> environment) {
        List<String> parts = new ArrayList<>();
        for (String key : REPRODUCE_KEYS) {
            String value = value(environment, key);
            if (!value.isEmpty()) {
                parts.add(key + "=" + value);
            }
        }
        parts.add(GRADLE_RUN);
        return String.join(" ", parts);
    }

    private static String value(Map<String, String> environment, String key) {
        String raw = environment.get(key);
        return raw == null ? "" : raw.trim();
    }

    private static String safeGet(Supplier<String> supplier) {
        try {
            String value = supplier.get();
            return value == null ? "" : value.trim();
        } catch (RuntimeException e) {
            return "";
        }
    }
}
