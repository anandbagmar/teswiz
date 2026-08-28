package com.znsio.teswiz.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

class ConfigurationFilesConsistencyTest {
    private static final Path CONFIG_ROOT = Path.of("configs");
    private static final Path TEMPLATE = CONFIG_ROOT.resolve("teswiz/teswiz_config.properties.template");
    private static final List<String> COMMON_PROPERTIES = List.of(
            "FRAMEWORK", "WEB_ENGINE", "ENVIRONMENT_CONFIG_FILE", "LOG_DIR", "LOG_PROPERTIES_FILE", "PARALLEL",
            "PLATFORM", "RUN_IN_CI", "TARGET_ENVIRONMENT", "TEST_DATA_FILE", "SHOW_SENSITIVE_DATA");

    @Test
    void everyExecutionConfigurationContainsTheCommonContract() throws IOException {
        try (Stream<Path> paths = Files.walk(CONFIG_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".properties"))
                    .forEach(this::assertCommonPropertiesPresent);
        }
    }

    @Test
    void everyExecutionConfigurationFollowsTheCanonicalTemplate() throws IOException {
        Set<String> templateProperties = templateProperties();
        try (Stream<Path> paths = Files.walk(CONFIG_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".properties"))
                    .forEach(path -> assertTemplatePropertiesPresent(path, templateProperties));
        }
    }

    private Set<String> templateProperties() throws IOException {
        Pattern propertyPattern = Pattern.compile("^#?([A-Z][A-Z0-9_]*)=.*$");
        try (Stream<String> lines = Files.lines(TEMPLATE)) {
            return lines.map(propertyPattern::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> matcher.group(1))
                    .collect(java.util.stream.Collectors.toSet());
        }
    }

    private void assertTemplatePropertiesPresent(Path configFile, Set<String> templateProperties) {
        try {
            String contents = Files.readString(configFile);
            assertThat(templateProperties)
                    .as("Missing template property in %s", configFile)
                    .allSatisfy(property -> assertThat(contents).containsPattern("(?m)^#?" + property + "="));
        } catch (IOException e) {
            throw new AssertionError("Unable to read configuration file: " + configFile, e);
        }
    }

    private void assertCommonPropertiesPresent(Path configFile) {
        try {
            String contents = Files.readString(configFile);
            assertThat(COMMON_PROPERTIES)
                    .as("Missing common property in %s", configFile)
                    .allSatisfy(property -> assertThat(contents).containsPattern("(?m)^" + property + "="));
        } catch (IOException e) {
            throw new AssertionError("Unable to read configuration file: " + configFile, e);
        }
    }
}
