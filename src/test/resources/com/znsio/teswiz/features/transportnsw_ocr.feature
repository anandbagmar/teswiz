@transportnsw @ocr @web
Feature: Visual OCR and Image Recognition map navigation on Transport NSW Metro page

  #CONFIG=./configs/transportnsw_ocr_config.properties ./gradlew run
  @web @positive
  Scenario: Visually navigate Metro interactive map using image matching and OCR callouts
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually click a station solid green dot on the map using image template "src/test/resources/images/solid_green_dot.png"
    And I visually click the callout action using OCR text "Departures from here"
    And I visually click the station name on the map using OCR text "Glenwood"
    Then I verify the station departures page for "Glenwood" is displayed
