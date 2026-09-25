# Teswiz Repository Rules

## Commit Message (required)

- Always provide a suggested Git commit message at the end of your response after completing any change or when requested.
- Use a concise, imperative mood summary line (max 50 chars).
- Include a blank line after the summary line.
- Provide a bulleted description of the files changed and what was modified.

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
