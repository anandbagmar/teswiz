package com.znsio.teswiz.api;

import com.znsio.teswiz.exceptions.InvalidTestDataException;

import java.util.Arrays;

public enum ApiEngine {
    REST_ASSURED("rest-assured"),
    PLAYWRIGHT_JAVA("playwright-java");

    private final String configValue;

    ApiEngine(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigValue() {
        return configValue;
    }

    public static ApiEngine from(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return REST_ASSURED;
        }
        return Arrays.stream(values())
                .filter(engine -> engine.configValue.equalsIgnoreCase(rawValue.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidTestDataException(
                        String.format("Unsupported API_ENGINE: '%s'. Supported values are: rest-assured, playwright-java",
                                rawValue)));
    }
}
