package com.znsio.teswiz.runner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TeswizConfiguration}, the injectable holder behind {@link Setup}'s static
 * config facade. Focuses on the safe typed accessors, especially the missing-key defaults that
 * avoid the {@link NullPointerException} unboxing a bare map would throw.
 */
class TeswizConfigurationTest {

    @Test
    void getIntegerReturnsZeroForMissingKey() {
        TeswizConfiguration configuration = new TeswizConfiguration();

        assertThat(configuration.getInteger("MISSING")).isZero();
    }

    @Test
    void getIntegerReturnsStoredValue() {
        TeswizConfiguration configuration = new TeswizConfiguration();
        configuration.putInteger("PARALLEL", 4);

        assertThat(configuration.getInteger("PARALLEL")).isEqualTo(4);
    }

    @Test
    void getIntegerAsStringReturnsZeroForMissingKey() {
        TeswizConfiguration configuration = new TeswizConfiguration();

        assertThat(configuration.getIntegerAsString("MISSING")).isEqualTo("0");
    }

    @Test
    void getBooleanReturnsFalseForMissingKey() {
        TeswizConfiguration configuration = new TeswizConfiguration();

        assertThat(configuration.getBoolean("MISSING")).isFalse();
    }

    @Test
    void getBooleanReturnsStoredValue() {
        TeswizConfiguration configuration = new TeswizConfiguration();
        configuration.putBoolean("HEADLESS", true);

        assertThat(configuration.getBoolean("HEADLESS")).isTrue();
    }

    @Test
    void getStringReturnsNullForMissingKeyAndDefaultWhenRequested() {
        TeswizConfiguration configuration = new TeswizConfiguration();

        assertThat(configuration.getString("MISSING")).isNull();
        assertThat(configuration.getStringOrDefault("MISSING", "chrome")).isEqualTo("chrome");
    }

    @Test
    void getStringReturnsStoredValueOverDefault() {
        TeswizConfiguration configuration = new TeswizConfiguration();
        configuration.putString("BROWSER", "firefox");

        assertThat(configuration.getStringOrDefault("BROWSER", "chrome")).isEqualTo("firefox");
    }

    @Test
    void clearRemovesAllTypedValues() {
        TeswizConfiguration configuration = new TeswizConfiguration();
        configuration.putString("BROWSER", "firefox");
        configuration.putBoolean("HEADLESS", true);
        configuration.putInteger("PARALLEL", 4);

        configuration.clear();

        assertThat(configuration.getString("BROWSER")).isNull();
        assertThat(configuration.getBoolean("HEADLESS")).isFalse();
        assertThat(configuration.getInteger("PARALLEL")).isZero();
    }
}
