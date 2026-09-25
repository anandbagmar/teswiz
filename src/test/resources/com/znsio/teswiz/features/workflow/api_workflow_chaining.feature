@api @workflow @api-chaining
Feature: API Chaining Workflow Scenarios

  @restassured-workflow
  Scenario: End-to-end API user lifecycle chaining with RestAssured engine
    Given I set the API engine to "rest-assured"
    When I create a new user post via API
    And I extract the created post ID from response
    And I fetch details of the post using the extracted ID
    And I update the title of the post using PUT
    And I verify response schema and latency is under 5000 ms
    Then I delete the post using DELETE and verify status code 200

  @pw-workflow
  Scenario: End-to-end API user lifecycle chaining with Playwright Java engine
    Given I set the API engine to "playwright-java"
    When I create a new user post via API
    And I extract the created post ID from response
    And I fetch details of the post using the extracted ID
    And I update the title of the post using PUT
    And I verify response schema and latency is under 5000 ms
    Then I delete the post using DELETE and verify status code 200
