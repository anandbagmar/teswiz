@web @api @workflow @hybrid-workflow @selenium @playwright-java @playwright-ts @restassured
Feature: Hybrid Web Automation & API Verification Workflow

  # WEB_ENGINE=selenium API_ENGINE=rest-assured HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@hybrid-workflow" ./gradlew run
  # WEB_ENGINE=playwright-java API_ENGINE=playwright-java HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@hybrid-workflow" ./gradlew run
  # WEB_ENGINE=playwright-ts API_ENGINE=playwright-java HEADLESS=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@hybrid-workflow" ./gradlew run
  @web @api @hybrid-workflow @selenium @playwright-java @playwright-ts @restassured
  Scenario: Hybrid Web session authentication followed by backend API verification
    Given I start the web session for parity testing
    When I add a session cookie "auth_token" with value "secret_123"
    And I send a GET request for post 1
    Then the status code should be 200
    And the cookie "auth_token" should be present in the browser session
