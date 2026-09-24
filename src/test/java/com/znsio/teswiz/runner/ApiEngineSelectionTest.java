package com.znsio.teswiz.runner;

import com.znsio.teswiz.api.ApiEngine;
import com.znsio.teswiz.exceptions.InvalidTestDataException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiEngineSelectionTest {

    @Test
    void defaultsToRestAssuredWhenEmptyOrNull() {
        assertThat(ApiEngine.from(null)).isEqualTo(ApiEngine.REST_ASSURED);
        assertThat(ApiEngine.from("")).isEqualTo(ApiEngine.REST_ASSURED);
        assertThat(ApiEngine.from("   ")).isEqualTo(ApiEngine.REST_ASSURED);
    }

    @Test
    void parsesSupportedEnginesCaseInsensitively() {
        assertThat(ApiEngine.from("rest-assured")).isEqualTo(ApiEngine.REST_ASSURED);
        assertThat(ApiEngine.from("REST-ASSURED")).isEqualTo(ApiEngine.REST_ASSURED);
        assertThat(ApiEngine.from("playwright-java")).isEqualTo(ApiEngine.PLAYWRIGHT_JAVA);
        assertThat(ApiEngine.from("PLAYWRIGHT-JAVA")).isEqualTo(ApiEngine.PLAYWRIGHT_JAVA);
    }

    @Test
    void throwsInvalidTestDataExceptionOnUnsupportedEngine() {
        assertThatThrownBy(() -> ApiEngine.from("invalid-engine"))
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("Unsupported API_ENGINE: 'invalid-engine'");
    }
}
