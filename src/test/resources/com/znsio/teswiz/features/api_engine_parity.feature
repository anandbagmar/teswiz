@api @engine-parity
Feature: API Engine Parity across RestAssured and Playwright Java Engines

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

  @pw-api
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
