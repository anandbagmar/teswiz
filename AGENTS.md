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

## Cucumber BDD Test Architecture

- Always follow the strict **Feature -> Step -> Business Layer (BL) -> Screen** design pattern for all Cucumber BDD test code generation:
  1. **Feature File** (`src/test/resources/com/znsio/teswiz/features/*.feature`): Gherkin BDD scenario definitions.
  2. **Step Definitions** (`src/test/java/com/znsio/teswiz/steps/*Steps.java`): Maps Gherkin steps to Business Layer (`*BL`) invocations.
  3. **Business Layer (BL)** (`src/test/java/com/znsio/teswiz/businessLayer/*/*BL.java`): Orchestrates business flow and assertions using abstract screen class `ScreenClass.get()`.
  4. **Screen Contract & Platform Concrete Classes** (`src/test/java/com/znsio/teswiz/screen/*/*Screen.java` and `src/test/java/com/znsio/teswiz/screen/<platform>/*/*Screen<Platform>.java`): Defines screen interaction contracts and platform-specific element interactions.

