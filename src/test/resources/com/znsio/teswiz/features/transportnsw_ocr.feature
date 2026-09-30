@transportnsw @ocr @web
Feature: Visual OCR and Image Recognition map navigation on Transport NSW Metro page
  #CONFIG=./configs/transportnsw_ocr_config.properties TAG="@transportnsw and @ocr" ./gradlew run

  @web @positive
  Scenario: Visually navigate Metro interactive map using image matching and OCR callouts
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually click "a station solid green dot on the map" using fallback OCR text "Rouse Hill" or image template "src/test/resources/images/solid_green_dot.png"
    And I visually click "Departures from here" using fallback OCR text "Departures from here" or image template "src/test/resources/images/departures_from_here.png"
    And I visually click "the station name on the map" using OCR text "Showground"
    Then I verify the station departures page for "Hills Showground" is displayed

  @web @positive1
  Scenario: Visually navigate Metro interactive map using image matching and OCR callouts - click on highway
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually click "a station solid green dot on the map" using fallback OCR text "Rouse Hill" or image template "src/test/resources/images/solid_green_dot.png"
    And I visually click "Departures from here" using fallback OCR text "Departures from here" or image template "src/test/resources/images/departures_from_here.png"
    Then I visually click "the motorway on the map" using fallback OCR text "A2" or image template "src/test/resources/images/motorway_a2.png"
