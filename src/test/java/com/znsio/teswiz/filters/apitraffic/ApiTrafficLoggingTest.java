package com.znsio.teswiz.filters.apitraffic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

/**
 * Unit tests for {@link ApiTrafficLogging} — the registrar. The feature is on by
 * default and disabled via the {@code API_TRAFFIC_LOGGING} flag.
 */
class ApiTrafficLoggingTest {

    @AfterEach
    void tearDown() {
        System.clearProperty(ApiTrafficLogging.API_TRAFFIC_LOGGING);
        RestAssured.replaceFiltersWith(List.of());
    }

    @Test
    void isEnabled_defaultsToTrueWhenFlagNotSet() {
        assertThat(ApiTrafficLogging.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_falseWhenFlagExplicitlyDisabled() {
        System.setProperty(ApiTrafficLogging.API_TRAFFIC_LOGGING, "false");
        assertThat(ApiTrafficLogging.isEnabled()).isFalse();
    }

    @Test
    void registerIfEnabled_registersFilterByDefault() {
        assertThat(ApiTrafficLogging.registerIfEnabled()).isTrue();
        assertThat(RestAssured.filters()).hasAtLeastOneElementOfType(ApiTrafficLoggingFilter.class);
    }

    @Test
    void registerIfEnabled_returnsFalseWhenExplicitlyDisabled() {
        System.setProperty(ApiTrafficLogging.API_TRAFFIC_LOGGING, "false");
        assertThat(ApiTrafficLogging.registerIfEnabled()).isFalse();
    }

    @Test
    void newFilter_buildsFilterBackedByTeswizScenarioDirectory() {
        ApiTrafficLoggingFilter filter = ApiTrafficLogging.newFilter();
        assertThat(filter).isNotNull();
    }
}
