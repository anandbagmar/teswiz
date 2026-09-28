@covidtracking @ocr @web
Feature: Visual OCR and Image Recognition on Covid Tracking site

  #CONFIG=./configs/covidtracking_ocr_config.properties TAG="@covidtracking and @ocr" ./gradlew run
  @web @positive
  Scenario: Visually inspect Covid Tracking dashboard metrics and state data using OCR and Image Recognition
    Given I navigate to the Covid Tracking dashboard at "https://covidtracking.in/"
    When I visually inspect the metrics card using OCR text "Confirmed"
    And I visually select a state using OCR text "Maharashtra"
    Then I visually verify the dashboard chart header using image template "src/test/resources/images/covid_chart_header.png"
