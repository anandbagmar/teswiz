@web @web-engine-parity @selenium @playwright-java @playwright-ts
Feature: Web Engine Parity and Capability Verification across Selenium, Playwright Java, and Playwright TS

  # WEB_ENGINE=selenium HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@parity-cookies" ./gradlew run
  # WEB_ENGINE=playwright-java HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@parity-cookies" ./gradlew run
  # WEB_ENGINE=playwright-ts HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@parity-cookies" ./gradlew run
  @web @parity-cookies @selenium @playwright-java @playwright-ts
  Scenario: Verify cookie management across all web engines
    Given I start the web session for parity testing
    When I add a session cookie "auth_token" with value "secret_123"
    Then the cookie "auth_token" should be present in the browser session
    When I delete the session cookie "auth_token"
    Then the cookie "auth_token" should not exist in the browser session

  # WEB_ENGINE=selenium HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@parity-viewport" ./gradlew run
  # WEB_ENGINE=playwright-java HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@parity-viewport" ./gradlew run
  # WEB_ENGINE=playwright-ts HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@parity-viewport" ./gradlew run
  @web @parity-viewport @selenium @playwright-java @playwright-ts
  Scenario: Verify viewport resizing across all web engines
    Given I start the web session for parity testing
    When I set the window viewport size to 1024 width and 768 height
    Then the window viewport size should be 1024 width and 768 height

  # WEB_ENGINE=selenium HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@parity-async" ./gradlew run
  # WEB_ENGINE=playwright-java HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@parity-async" ./gradlew run
  # WEB_ENGINE=playwright-ts HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@parity-async" ./gradlew run
  @web @parity-async @selenium @playwright-java @playwright-ts
  Scenario: Verify async script execution with timeout protection across all web engines
    Given I start the web session for parity testing
    When I execute an async script with 50ms delay returning "async-completed"
    Then the async script execution result should be "async-completed"
