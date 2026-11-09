package com.znsio.teswiz.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * build.gradle.sample is what consumer projects copy. Their layout (step definitions, features,
 * screen contracts, default config) must be configurable from gradle.properties, while the defaults
 * keep the existing com.znsio.teswiz layout so current copies behave the same.
 */
class BuildGradleSampleTest {
    private static final Path SAMPLE = Path.of("build.gradle.sample");

    @Test
    void shouldLetProjectsOverrideTheirLayoutFromGradleProperties() throws IOException {
        String sample = Files.readString(SAMPLE);

        assertThat(sample)
                .contains("findProperty(\"teswizStepsPackagePath\") ?: \"com/znsio/teswiz/steps\"")
                .contains("findProperty(\"teswizFeaturesDir\") ?: \"./src/test/resources/com/znsio/teswiz/features\"")
                .contains("findProperty(\"teswizScreenContractsRoot\") ?: \"src/test/java/com/znsio/teswiz/screen\"")
                .contains("System.getenv(\"CONFIG\") ?: findProperty(\"teswizConfig\")");
    }

    @Test
    void shouldPassTheConfiguredLayoutToTheRunner() throws IOException {
        String sample = Files.readString(SAMPLE);

        assertThat(sample).containsPattern("\"\\$\\{configFile\\}\",\\s*teswizStepsPackagePath,\\s*teswizFeaturesDir");
    }

    @Test
    void shouldGiveProjectsAScreenContractCheck() throws IOException {
        String sample = Files.readString(SAMPLE);

        assertThat(sample)
                .contains("tasks.register('verifyScreenContracts', JavaExec)")
                .contains("mainClass = \"com.znsio.teswiz.screen.ScreenContractSanityChecker\"")
                .contains("args teswizScreenContractsRoot");
    }
}
