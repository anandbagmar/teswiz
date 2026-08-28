package com.znsio.teswiz.filters.apitraffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.tools.OverriddenVariable;

import io.restassured.RestAssured;

/**
 * Registration for per-scenario API traffic logging.
 * <p>
 * teswiz registers a global {@link ApiTrafficLoggingFilter} so every RestAssured
 * call is captured as a masked, per-call file under the scenario's report folder.
 * The feature is on by default; set {@code API_TRAFFIC_LOGGING=false} (config
 * property or env var) to opt out, mirroring the {@code DISABLE_ENVIRONMENT_ISSUE_FILTER}
 * opt-out convention used for the {@link com.znsio.teswiz.filters.EnvironmentIssueFilter}.
 * <p>
 * teswiz owns the per-scenario call counter lifecycle (reset at scenario start,
 * cleared at scenario finish) via {@code CucumberScenarioListener}, so consuming
 * projects need only set the flag when they want to turn it off.
 */
public final class ApiTrafficLogging {

    public static final String API_TRAFFIC_LOGGING = "API_TRAFFIC_LOGGING";

    private static final Logger LOGGER = LogManager.getLogger(ApiTrafficLogging.class.getName());

    private ApiTrafficLogging() {
    }

    /**
     * Returns {@code true} when API traffic logging is enabled — the default,
     * unless explicitly disabled via config or env var.
     */
    public static boolean isEnabled() {
        return OverriddenVariable.getOverriddenBooleanValue(API_TRAFFIC_LOGGING, true);
    }

    /**
     * Registers the global api-traffic filter when the feature is enabled.
     *
     * @return {@code true} when the filter was registered, {@code false} when disabled
     */
    public static boolean registerIfEnabled() {
        if (!isEnabled()) {
            return false;
        }
        RestAssured.filters(newFilter());
        LOGGER.info("API traffic logging enabled: per-call request/response files will be written to each scenario's report folder");
        return true;
    }

    /**
     * Builds a configured filter that writes into teswiz's per-scenario report folder.
     */
    public static ApiTrafficLoggingFilter newFilter() {
        ApiTrafficRecorder recorder = new ApiTrafficRecorder(new TeswizScenarioDirectoryResolver());
        return new ApiTrafficLoggingFilter(recorder);
    }
}
