package com.znsio.teswiz.analytics;

import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.tools.cmd.CommandLineExecutor;
import com.znsio.teswiz.tools.cmd.CommandLineResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

import static com.znsio.teswiz.tools.OverriddenVariable.getOverriddenBooleanValue;
import static com.znsio.teswiz.tools.OverriddenVariable.getOverriddenStringValue;

public class RunTelemetry {
    private static final Logger LOGGER = LogManager.getLogger(RunTelemetry.class.getName());
    private static final String RUN_ID = UUID.randomUUID().toString();
    private static String cachedNodeVersion = null;
    private static String cachedProjectIdentifier = null;
    private static String cachedOrgIdentifier = null;

    public static String getRunId() {
        return RUN_ID;
    }

    public static JSONObject buildStartEventPayload() {
        TelemetryMode mode = TelemetryConfig.getTelemetryMode();
        JSONObject root = new JSONObject();
        root.put("api_key", TelemetryConfig.getTelemetryApiKey());
        root.put("event", "teswiz_run_started");
        root.put("distinct_id", getDistinctId(mode));

        JSONObject props = new JSONObject();
        props.put("run_id", RUN_ID);
        props.put("teswiz_version", getTeswizVersion());
        props.put("project_identifier", getProjectIdentifier(mode));

        String orgName = getOrganizationIdentifier(mode);
        if (!"NOT_SET".equalsIgnoreCase(orgName)) {
            props.put("org_identifier", orgName);
        }

        JSONObject executionConfig = new JSONObject();
        executionConfig.put("framework", Setup.getStringValueFromConfigs(Setup.FRAMEWORK, "cucumber"));
        executionConfig.put("platform", Setup.getPlatform() != null ? Setup.getPlatform().name().toLowerCase() : "unknown");
        executionConfig.put("web_engine", Setup.getStringValueFromConfigs(Setup.WEB_ENGINE, "selenium"));
        executionConfig.put("api_engine", Setup.getStringValueFromConfigs(Setup.API_ENGINE, "rest-assured"));
        executionConfig.put("browser", Setup.getStringValueFromConfigs(Setup.BROWSER, "chrome"));
        executionConfig.put("parallel_count", Setup.getIntegerValueAsStringFromConfigs(Setup.PARALLEL));
        executionConfig.put("headless", Setup.getBooleanValueFromConfigs(Setup.HEADLESS));
        props.put("execution_config", executionConfig);

        JSONObject runEnvironment = new JSONObject();
        runEnvironment.put("os", System.getProperty("os.name"));
        runEnvironment.put("java_version", System.getProperty("java.version"));
        runEnvironment.put("node_version", getNodeVersion());
        runEnvironment.put("is_ci", getOverriddenBooleanValue(Setup.RUN_IN_CI, false));
        runEnvironment.put("ci_provider", detectCiProvider());
        runEnvironment.put("is_docker", isDockerEnvironment());
        runEnvironment.put("cloud_provider", detectCloudProvider());
        runEnvironment.put("use_proxy", isProxyConfigured());
        props.put("run_environment", runEnvironment);

        JSONObject subsystemAdoption = new JSONObject();
        subsystemAdoption.put("is_visual_enabled", Runner.isVisualTestingEnabled());
        subsystemAdoption.put("is_ocr_enabled", getOverriddenBooleanValue(Setup.IS_OCR_ENABLED, false));
        subsystemAdoption.put("highlight_elements", getOverriddenBooleanValue(Setup.HIGHLIGHT_ELEMENTS, true));
        props.put("subsystem_adoption", subsystemAdoption);

        root.put("properties", props);
        return root;
    }

    public static JSONObject buildCompletedEventPayload(int totalScenarios, int passedScenarios, int failedScenarios, long durationMs) {
        TelemetryMode mode = TelemetryConfig.getTelemetryMode();
        JSONObject root = new JSONObject();
        root.put("api_key", TelemetryConfig.getTelemetryApiKey());
        root.put("event", "teswiz_run_completed");
        root.put("distinct_id", getDistinctId(mode));

        JSONObject props = new JSONObject();
        props.put("run_id", RUN_ID);
        props.put("teswiz_version", getTeswizVersion());
        props.put("project_identifier", getProjectIdentifier(mode));

        String orgName = getOrganizationIdentifier(mode);
        if (!"NOT_SET".equalsIgnoreCase(orgName)) {
            props.put("org_identifier", orgName);
        }

        props.put("framework", Setup.getStringValueFromConfigs(Setup.FRAMEWORK, "cucumber"));
        props.put("platform", Setup.getPlatform() != null ? Setup.getPlatform().name().toLowerCase() : "unknown");
        props.put("web_engine", Setup.getStringValueFromConfigs(Setup.WEB_ENGINE, "selenium"));
        props.put("api_engine", Setup.getStringValueFromConfigs(Setup.API_ENGINE, "rest-assured"));
        props.put("is_ci", Setup.getBooleanValueFromConfigs(Setup.RUN_IN_CI));

        JSONObject runMetrics = new JSONObject();
        runMetrics.put("total_scenarios", totalScenarios);
        runMetrics.put("passed", passedScenarios);
        runMetrics.put("failed", failedScenarios);
        runMetrics.put("duration_ms", durationMs);
        runMetrics.put("duration_seconds", String.format("%.2f", durationMs / 1000.0));
        runMetrics.put("status", failedScenarios == 0 ? "success" : "failed");
        props.put("run_metrics", runMetrics);

        root.put("properties", props);
        return root;
    }

    private static String getDistinctId(TelemetryMode mode) {
        return getProjectIdentifier(mode);
    }

    private static String getProjectIdentifier(TelemetryMode mode) {
        if (cachedProjectIdentifier != null) {
            return cachedProjectIdentifier;
        }
        String rawProjectName = resolveRawProjectName();
        if (mode == TelemetryMode.ENABLED_MASKED) {
            cachedProjectIdentifier = DigestUtils.sha256Hex(rawProjectName);
        } else {
            cachedProjectIdentifier = rawProjectName;
        }
        return cachedProjectIdentifier;
    }

    private static String getOrganizationIdentifier(TelemetryMode mode) {
        if (cachedOrgIdentifier != null) {
            return cachedOrgIdentifier;
        }
        String customOrg = TelemetryConfig.getOrganizationName();
        if (!"NOT_SET".equalsIgnoreCase(customOrg)) {
            cachedOrgIdentifier = (mode == TelemetryMode.ENABLED_MASKED) ? DigestUtils.sha256Hex(customOrg) : customOrg;
            return cachedOrgIdentifier;
        }
        String rawOrg = resolveRawOrgName();
        if (!"NOT_SET".equalsIgnoreCase(rawOrg)) {
            cachedOrgIdentifier = (mode == TelemetryMode.ENABLED_MASKED) ? DigestUtils.sha256Hex(rawOrg) : rawOrg;
        } else {
            cachedOrgIdentifier = "NOT_SET";
        }
        return cachedOrgIdentifier;
    }

    private static String resolveRawProjectName() {
        try {
            File userDir = new File(System.getProperty("user.dir"));
            return userDir.getName();
        } catch (Exception e) {
            return "teswiz-client-project";
        }
    }

    private static String resolveRawOrgName() {
        try {
            CommandLineResponse response = CommandLineExecutor.execCommand(new String[]{"git", "config", "--get", "remote.origin.url"});
            String remoteUrl = response.getStdOut().trim();
            if (!remoteUrl.isEmpty()) {
                if (remoteUrl.contains("github.com/") || remoteUrl.contains("gitlab.com/")) {
                    String[] parts = remoteUrl.split("[:/]");
                    if (parts.length >= 2) {
                        return parts[parts.length - 2];
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "NOT_SET";
    }

    private static String getNodeVersion() {
        if (cachedNodeVersion != null) {
            return cachedNodeVersion;
        }
        try {
            CommandLineResponse response = CommandLineExecutor.execCommand(new String[]{"node", "-v"});
            String stdOut = response.getStdOut().trim();
            if (!stdOut.isEmpty() && stdOut.startsWith("v")) {
                cachedNodeVersion = stdOut;
            } else {
                cachedNodeVersion = "N/A";
            }
        } catch (Exception e) {
            cachedNodeVersion = "N/A";
        }
        return cachedNodeVersion;
    }

    private static String detectCiProvider() {
        if (System.getenv("GITHUB_ACTIONS") != null) return "github_actions";
        if (System.getenv("JENKINS_URL") != null) return "jenkins";
        if (System.getenv("GITLAB_CI") != null) return "gitlab";
        if (System.getenv("TF_BUILD") != null) return "azure_devops";
        if (System.getenv("CIRCLECI") != null) return "circleci";
        if (System.getenv("BITBUCKET_COMMIT") != null) return "bitbucket";
        if (getOverriddenBooleanValue(Setup.RUN_IN_CI, false)) return "ci_generic";
        return "local";
    }

    private static boolean isDockerEnvironment() {
        return new File("/.dockerenv").exists() || new File("/run/.containerenv").exists();
    }

    private static String detectCloudProvider() {
        try {
            String cloudName = getOverriddenStringValue("CLOUD_NAME", "NOT_SET");
            if (!"NOT_SET".equalsIgnoreCase(cloudName)) {
                return cloudName.toLowerCase();
            }
            if (getOverriddenBooleanValue(Setup.RUN_IN_CI, false)) {
                return "cloud_ci";
            }
        } catch (Exception ignored) {
        }
        return "local";
    }

    private static boolean isProxyConfigured() {
        String proxyUrl = getOverriddenStringValue(Setup.PROXY_URL, "NOT_SET");
        String proxyKey = getOverriddenStringValue("PROXY_KEY", "NOT_SET");
        return !"NOT_SET".equalsIgnoreCase(proxyUrl) || System.getenv(proxyKey) != null;
    }

    private static String getTeswizVersion() {
        String version = RunTelemetry.class.getPackage().getImplementationVersion();
        return version != null ? version : "1.0.40";
    }
}
