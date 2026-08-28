package com.znsio.teswiz.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ConfigurationFilesConsistencyTest {
    private static final Path CONFIG_ROOT = Path.of("configs");
    private static final Path TEMPLATE = CONFIG_ROOT.resolve("teswiz/teswiz_config.properties.template");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("^#?([A-Z][A-Z0-9_]*)=.*$");
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
        List<String> templateProperties = templateProperties();
        try (Stream<Path> paths = Files.walk(CONFIG_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".properties"))
                    .forEach(path -> assertTemplatePropertiesPresent(path, templateProperties));
        }
    }

    @Test
    void everyExecutionConfigurationKeepsCanonicalTemplateOrder() throws IOException {
        List<String> templateProperties = templateProperties();
        Set<String> templatePropertySet = Set.copyOf(templateProperties);
        try (Stream<Path> paths = Files.walk(CONFIG_ROOT)) {
            paths.filter(path -> path.toString().endsWith(".properties"))
                    .forEach(path -> assertTemplatePropertyOrder(path, templateProperties, templatePropertySet));
        }
    }

    private List<String> templateProperties() throws IOException {
        try (Stream<String> lines = Files.lines(TEMPLATE)) {
            return lines.map(PROPERTY_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> matcher.group(1))
                    .toList();
        }
    }

    private void assertTemplatePropertiesPresent(Path configFile, List<String> templateProperties) {
        try {
            String contents = Files.readString(configFile);
            assertThat(templateProperties)
                    .as("Missing template property in %s", configFile)
                    .allSatisfy(property -> assertThat(contents).containsPattern("(?m)^#?" + property + "="));
        } catch (IOException e) {
            throw new AssertionError("Unable to read configuration file: " + configFile, e);
        }
    }

    private void assertTemplatePropertyOrder(Path configFile, List<String> templateProperties,
            Set<String> templatePropertySet) {
        try (Stream<String> lines = Files.lines(configFile)) {
            List<String> configProperties = new ArrayList<>();
            lines.map(PROPERTY_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> matcher.group(1))
                    .filter(templatePropertySet::contains)
                    .forEach(configProperties::add);

            assertThat(configProperties)
                    .as("Canonical properties must appear once and in template order in %s", configFile)
                    .containsExactlyElementsOf(templateProperties);
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
