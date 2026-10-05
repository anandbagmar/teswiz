package com.znsio.teswiz.analytics;

import com.znsio.teswiz.runner.Setup;
import static com.znsio.teswiz.tools.OverriddenVariable.getOverriddenStringValue;

public class TelemetryConfig {
    public static final String DEFAULT_ENDPOINT = "https://us.i.posthog.com/capture/";
    public static final String DEFAULT_API_KEY = "phc_BJLtwKR55tVhVczrttZmPBKeNowKknu8arctBWdsM5VH";

    public static TelemetryMode getTelemetryMode() {
        String modeStr = getOverriddenStringValue(Setup.ENABLE_USAGE_ANALYTICS, TelemetryMode.ENABLED.getValue());
        return TelemetryMode.fromString(modeStr);
    }

    public static boolean isTelemetryEnabled() {
        return getTelemetryMode() != TelemetryMode.DISABLED;
    }

    public static String getTelemetryEndpoint() {
        return getOverriddenStringValue(Setup.TELEMETRY_ENDPOINT, DEFAULT_ENDPOINT);
    }

    public static String getTelemetryApiKey() {
        return getOverriddenStringValue(Setup.TELEMETRY_API_KEY, DEFAULT_API_KEY);
    }

    public static String getOrganizationName() {
        return getOverriddenStringValue(Setup.ORGANIZATION_NAME, "NOT_SET");
    }
}
