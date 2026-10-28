package com.znsio.teswiz.reporting.notify;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The provenance of a single run, captured for the notification subject and footer so a recipient can trace a result
 * back to where it ran. A CI run carries the pipeline and job identifiers + URLs, the branch, the short commit, who
 * triggered it and which runner executed it; a local run carries the host machine name and the logged-in user. Both
 * carry the detected CI provider name, the platform (web/api) and target environment (for the subject prefix). The
 * platform and target environment prefer the injected environment map but fall back to an injected configuration
 * lookup, because on a local run they come from {@code config.properties} rather than the shell environment. The lookup
 * is injected rather than read from a global singleton so resolution stays deterministic and unit-testable offline.
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
     * Resolves the run provenance from the environment, the already-resolved branch, and the host / user suppliers,
     * reading {@code PLATFORM} / {@code TARGET_ENVIRONMENT} from the injected environment map only. This overload holds
     * no dependency on any global configuration singleton, so it is fully deterministic and unit-testable offline. The
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
        return resolve(environment, branch, hostSupplier, userSupplier, key -> null);
    }

    /**
     * Resolves the run provenance as {@link #resolve(Map, String, Supplier, Supplier)} does, but with an explicit
     * configuration lookup used as the fallback for {@code PLATFORM} / {@code TARGET_ENVIRONMENT} when the injected
     * environment map has no value. These rarely live in the shell environment (on a local run they come from
     * {@code config.properties}, not {@code System.getenv()}), so a real run passes teswiz's merged configuration
     * lookup ({@code Setup::getFromConfigs}) here. The lookup is injected rather than read from a global singleton so
     * resolution stays deterministic offline — the no-fallback overload passes a lookup that returns {@code null}.
     *
     * @param environment  the process environment (CI vars + run inputs); the reporter passes {@code System.getenv()}
     * @param branch       the branch the reporter already resolved (CI ref else git); reused here, not re-derived
     * @param hostSupplier the OS host-name supplier (used only for a local run)
     * @param userSupplier the logged-in user-name supplier (used only for a local run)
     * @param configLookup the fallback lookup for platform/environment; may return {@code null} when a key is unset
     * @return the resolved, immutable run environment
     */
    public static RunEnvironment resolve(Map<String, String> environment, String branch, Supplier<String> hostSupplier,
            Supplier<String> userSupplier, Function<String, String> configLookup) {
        CiProvider provider = CiProvider.detect(environment);
        boolean ci = provider.isCi();
        return new RunEnvironment(ci, provider.displayName(), resolvedValue(environment, PLATFORM, configLookup),
                resolvedValue(environment, TARGET_ENVIRONMENT, configLookup), provider.pipelineId(environment),
                provider.pipelineUrl(environment), provider.jobId(environment), provider.jobName(environment),
                provider.jobUrl(environment), branch == null ? "" : branch.trim(), provider.commit(environment),
                provider.triggeredBy(environment), provider.runner(environment), ci ? "" : safeGet(hostSupplier),
                ci ? "" : safeGet(userSupplier));
    }

    /**
     * Resolves {@code PLATFORM} / {@code TARGET_ENVIRONMENT} for the subject prefix. The injected environment map is
     * the first choice; when it has no value we fall back to the injected configuration lookup, which the no-fallback
     * overload wires to always return {@code null} and a real run wires to teswiz's merged configuration.
     */
    private static String resolvedValue(Map<String, String> environment, String key,
            Function<String, String> configLookup) {
        String raw = environment.get(key);
        if (raw != null && !raw.trim().isEmpty()) {
            return raw.trim();
        }
        return fromConfigLookup(configLookup, key);
    }

    private static String fromConfigLookup(Function<String, String> configLookup, String key) {
        try {
            String value = configLookup.apply(key);
            return value == null ? "" : value.trim();
        } catch (RuntimeException e) {
            return "";
        }
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
