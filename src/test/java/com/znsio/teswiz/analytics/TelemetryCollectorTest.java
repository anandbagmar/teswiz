package com.znsio.teswiz.analytics;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TelemetryCollectorTest {

    @BeforeEach
    void setUp() {
        TelemetryCollector.resetForTesting();
    }

    @Test
    void testTelemetryModeParsing() {
        assertThat(TelemetryMode.fromString("enabled")).isEqualTo(TelemetryMode.ENABLED);
        assertThat(TelemetryMode.fromString("enabled-masked")).isEqualTo(TelemetryMode.ENABLED_MASKED);
        assertThat(TelemetryMode.fromString("disabled")).isEqualTo(TelemetryMode.DISABLED);
        assertThat(TelemetryMode.fromString(null)).isEqualTo(TelemetryMode.ENABLED);
        assertThat(TelemetryMode.fromString("false")).isEqualTo(TelemetryMode.DISABLED);
    }

    @Test
    void testBuildStartEventPayloadStructure() {
        JSONObject payload = RunTelemetry.buildStartEventPayload();
        assertThat(payload.getString("event")).isEqualTo("teswiz_run_started");
        assertThat(payload.has("api_key")).isTrue();
        assertThat(payload.has("distinct_id")).isTrue();

        JSONObject props = payload.getJSONObject("properties");
        assertThat(props.has("run_id")).isTrue();
        assertThat(props.has("execution_config")).isTrue();
        assertThat(props.has("run_environment")).isTrue();
        assertThat(props.has("subsystem_adoption")).isTrue();
    }

    @Test
    void testBuildCompletedEventPayloadStructure() {
        JSONObject payload = RunTelemetry.buildCompletedEventPayload(10, 8, 2, 15000);
        assertThat(payload.getString("event")).isEqualTo("teswiz_run_completed");
        assertThat(payload.has("distinct_id")).isTrue();

        JSONObject props = payload.getJSONObject("properties");
        assertThat(props.has("run_metrics")).isTrue();
        JSONObject metrics = props.getJSONObject("run_metrics");
        assertThat(metrics.getInt("total_scenarios")).isEqualTo(10);
        assertThat(metrics.getInt("passed")).isEqualTo(8);
        assertThat(metrics.getInt("failed")).isEqualTo(2);
        assertThat(metrics.getString("status")).isEqualTo("failed");
    }
}
