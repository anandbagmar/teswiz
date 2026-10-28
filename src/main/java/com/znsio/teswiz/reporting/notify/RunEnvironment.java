package com.znsio.teswiz.reporting.notify;

import java.util.Map;
import java.util.function.Supplier;

/**
 * The provenance of a single run, captured for the notification subject and footer so a recipient can trace a result
 * back to where it ran. A CI run carries the pipeline and job identifiers + URLs, the branch, the short commit, who
 * triggered it and which runner executed it; a local run carries the host machine name and the logged-in user. Both
 * carry the detected CI provider name, the platform (web/api) and target environment (for the subject prefix).
 * <p>
 * It deliberately carries no "reproduce command": which run inputs identify a run is domain knowledge teswiz does not
 * have (it has no notion of a region/market/brand/integration tuple), so a consuming project composes that string
 * itself — from the generic {@link HtmlReport#reproduceCommand(Map)} builder if it wants shell-safe quoting — and
 * passes it to {@link HtmlReport#footer(RunEnvironment, String)} /
 * {@link HtmlReport#slackFooter(RunEnvironment, String)}.
 * <p>
 * The CI facts are resolved generically through {@link CiProvider}: teswiz detects whichever CI system produced the run
 * (GitLab, GitHub Actions, Jenkins, CircleCI, Bitbucket, Azure Pipelines, …) from its signature environment variable
 * and reads that provider's native variable names, so this is no longer GitLab-specific. An unrecognised environment
 * resolves as a local run.
 * <p>
 * {@link #resolve} reads everything from an injected environment map, the already-resolved branch, and a host-name and
 * user-name supplier, so the resolution is deterministic and unit-testable offline; the reporter passes
 * {@code System.getenv()}, the branch it already resolved (CI ref else git), and the OS host / user suppliers. The
 * branch is passed in rather than re-derived here so branch resolution lives in exactly one place.
 */
public record RunEnvironment(boolean isCi, String ciProvider, String platform, String environment, String pipelineId,
        String pipelineUrl, String jobId, String jobName, String jobUrl, String branch, String commit,
        String triggeredBy, String runner, String host, String user) {

    private static final String PLATFORM = "PLATFORM";
    private static final String TARGET_ENVIRONMENT = "TARGET_ENVIRONMENT";

    /**
     * Resolves the run provenance from the environment, the already-resolved branch, and the host / user suppliers. The
     * CI provider and its fields are resolved generically via {@link CiProvider#detect(Map)}.
     *
     * @param environment  the process environment (CI vars + run inputs); the reporter passes {@code System.getenv()}
     * @param branch       the branch the reporter already resolved (CI ref else git); reused here, not re-derived
     * @param hostSupplier the OS host-name supplier (used only for a local run)
     * @param userSupplier the logged-in user-name supplier (used only for a local run)
     * @return the resolved, immutable run environment
     */
    public static RunEnvironment resolve(Map<String, String> environment, String branch, Supplier<String> hostSupplier,
            Supplier<String> userSupplier) {
        CiProvider provider = CiProvider.detect(environment);
        boolean ci = provider.isCi();
        return new RunEnvironment(ci, provider.displayName(), value(environment, PLATFORM),
                value(environment, TARGET_ENVIRONMENT), provider.pipelineId(environment),
                provider.pipelineUrl(environment), provider.jobId(environment), provider.jobName(environment),
                provider.jobUrl(environment), branch == null ? "" : branch.trim(), provider.commit(environment),
                provider.triggeredBy(environment), provider.runner(environment), ci ? "" : safeGet(hostSupplier),
                ci ? "" : safeGet(userSupplier));
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
