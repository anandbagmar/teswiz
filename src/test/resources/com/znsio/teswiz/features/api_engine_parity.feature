@api @engine-parity @restassured @playwright-java @playwright-ts
Feature: API Engine Parity across RestAssured, Playwright Java, and Playwright TS Engines

  # Run with RestAssured:
  # API_ENGINE=rest-assured CONFIG=./configs/api_local_config.properties TAG="@engine-parity" PLATFORM=api ./gradlew run
  #
  # Run with Playwright Java:
  # API_ENGINE=playwright-java CONFIG=./configs/api_local_config.properties TAG="@engine-parity" PLATFORM=api ./gradlew run
  #
  # Run with Playwright TS (web & API suite):
  # WEB_ENGINE=playwright-ts API_ENGINE=playwright-java CONFIG=./configs/theapp/theapp_local_web_config.properties TAG="@engine-parity" ./gradlew run

  @engine-parity @restassured @playwright-java @playwright-ts
  Scenario: Validate full HTTP operations suite across configured API engine
    Given I use the configured API engine
    When I send a GET request for post 1
    Then the status code should be 200
    When I send a POST request to create a post
    Then the status code should be 201
    When I send a PUT request to replace post 1
    Then the status code should be 200
    When I send a PATCH request to modify post 1
    Then the status code should be 200
    When I send a DELETE request for post 1
    Then the status code should be 200
    When I send a HEAD request for post 1
    Then the status code should be 200
    When I send an OPTIONS request for posts
    Then the status code should be less than 400
    When I request an HTML webpage
    Then the response should contain HTML content
