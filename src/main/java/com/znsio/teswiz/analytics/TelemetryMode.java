package com.znsio.teswiz.analytics;

public enum TelemetryMode {
    ENABLED("enabled"),
    ENABLED_MASKED("enabled-masked"),
    DISABLED("disabled");

    private final String value;

    TelemetryMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TelemetryMode fromString(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return ENABLED;
        }
        String normalized = rawValue.trim().toLowerCase();
        for (TelemetryMode mode : values()) {
            if (mode.value.equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        if ("false".equals(normalized) || "off".equals(normalized)) {
            return DISABLED;
        }
        if ("true".equals(normalized) || "on".equals(normalized)) {
            return ENABLED;
        }
        return ENABLED;
    }
}
