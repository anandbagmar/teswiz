@pw-api @api
Feature: Playwright API Engine Tests

  Scenario: Execute GET request using Playwright API Engine
    Given I send a GET request using Playwright API engine
    Then the response status code should be 200

  Scenario: Execute POST request using Playwright API Engine
    Given I send a POST request with payload using Playwright API engine
    Then the created post status code should be 201
