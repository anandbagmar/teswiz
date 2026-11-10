package com.znsio.teswiz.config;

import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.runner.TeswizConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TeswizConfig#reader()} lets library packages read the execution configuration without
 * depending on {@code com.znsio.teswiz.runner}. It must answer exactly as the equivalent
 * {@code Setup.getXxxFromConfigs} call does, so migrating a call site cannot change behaviour.
 */
class TeswizConfigTest {
    private static final String STRING_KEY = "TESWIZ_CONFIG_TEST_STRING";
    private static final String BOOLEAN_KEY = "TESWIZ_CONFIG_TEST_BOOLEAN";
    private static final String MISSING_KEY = "TESWIZ_CONFIG_TEST_MISSING";

    @Test
    void readerIsTheConfigurationSetupUses() {
        Setup.addToConfigs(STRING_KEY, "value-from-setup");
        Setup.addBooleanValueToConfigs(BOOLEAN_KEY, true);

        TeswizConfigReader reader = TeswizConfig.reader();

        assertThat(reader.getString(STRING_KEY)).isEqualTo(Setup.getFromConfigs(STRING_KEY))
                .isEqualTo("value-from-setup");
        assertThat(reader.getStringOrDefault(MISSING_KEY, "fallback"))
                .isEqualTo(Setup.getStringValueFromConfigs(MISSING_KEY, "fallback"));
        assertThat(reader.getBoolean(BOOLEAN_KEY)).isEqualTo(Setup.getBooleanValueFromConfigs(BOOLEAN_KEY)).isTrue();
        assertThat(reader.getBooleanAsString(BOOLEAN_KEY))
                .isEqualTo(Setup.getBooleanValueAsStringFromConfigs(BOOLEAN_KEY));
        assertThat(reader.getInteger(MISSING_KEY)).isEqualTo(Setup.getIntegerValueFromConfigs(MISSING_KEY));
        assertThat(reader.getIntegerAsString(MISSING_KEY))
                .isEqualTo(Setup.getIntegerValueAsStringFromConfigs(MISSING_KEY));
    }

    @Test
    void readerSeesLaterChangesToTheConfiguration() {
        TeswizConfigReader reader = TeswizConfig.reader();

        Setup.addToConfigs(STRING_KEY, "changed-after-lookup");

        assertThat(reader.getString(STRING_KEY)).isEqualTo("changed-after-lookup");
    }

    @ParameterizedTest
    @ValueSource(strings = {MISSING_KEY, "ANY_OTHER_KEY"})
    void emptyReaderAnswersLikeAnEmptyConfiguration(String key) {
        TeswizConfigReader empty = TeswizConfig.emptyReader();
        TeswizConfiguration emptyConfiguration = new TeswizConfiguration();

        assertThat(empty.getString(key)).isEqualTo(emptyConfiguration.getString(key)).isNull();
        assertThat(empty.getStringOrDefault(key, "d")).isEqualTo(emptyConfiguration.getStringOrDefault(key, "d"));
        assertThat(empty.getBoolean(key)).isEqualTo(emptyConfiguration.getBoolean(key)).isFalse();
        assertThat(empty.getBooleanAsString(key)).isEqualTo(emptyConfiguration.getBooleanAsString(key));
        assertThat(empty.getInteger(key)).isEqualTo(emptyConfiguration.getInteger(key)).isZero();
        assertThat(empty.getIntegerAsString(key)).isEqualTo(emptyConfiguration.getIntegerAsString(key));
    }

    @Test
    void teswizConfigurationIsAReader() {
        assertThat(new TeswizConfiguration()).isInstanceOf(TeswizConfigReader.class);
    }
}
