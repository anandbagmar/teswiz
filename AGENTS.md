# Teswiz Repository Rules

## Execution configuration files

- Treat `configs/teswiz/teswiz_config.properties.template` as the canonical contract.
- Every `configs/**/*.properties` file must contain every template property, either active or
  commented.
- Preserve the active values already used by each example configuration.
- Keep properties in template order; place project- or provider-specific properties after the
  canonical properties.
- When adding a supported property, update the canonical template before updating example files.
- Keep unused properties commented with the default value and useful supported alternatives.
- Run `./gradlew validateConfigurationTemplates` after configuration changes. The validation is
  also part of `./gradlew test`, `./gradlew check`, `./gradlew build`, `./gradlew shadowJar`,
  and CI builds.
