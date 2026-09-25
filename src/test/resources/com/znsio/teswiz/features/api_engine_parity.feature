@api @engine-parity @playwright-ts
Feature: API Engine Parity across RestAssured, Playwright Java, and Playwright TS Engines

  # API_ENGINE=rest-assured CONFIG=./configs/api_local_config.properties TAG=@restassured-api PLATFORM=api ./gradlew run
  @restassured-api
  Scenario: Validate full HTTP operations suite using RestAssured engine
    Given I set the API engine to "rest-assured"
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

  # API_ENGINE=playwright-java CONFIG=./configs/api_local_config.properties TAG=@pw-api PLATFORM=api ./gradlew run
  @pw-api @playwright-java
  Scenario: Validate full HTTP operations suite using Playwright Java engine
    Given I set the API engine to "playwright-java"
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

  # WEB_ENGINE=playwright-ts API_ENGINE=playwright-java CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@playwright-ts-api" ./gradlew run
  @playwright-ts @playwright-ts-api @web
  Scenario: Validate API testing within Playwright TypeScript web automation suite
    Given I set the API engine to "playwright-java"
    When I send a GET request for post 1
    Then the status code should be 200
    When I send a POST request to create a post
    Then the status code should be 201
