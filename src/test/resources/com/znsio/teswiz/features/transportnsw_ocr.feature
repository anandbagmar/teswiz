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
  Scenario: Visually navigate Metro interactive map using image matching and OCR callouts - using OCR text
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually click "a station solid green dot on the map" using fallback OCR text "Rouse Hill" or image template "src/test/resources/images/solid_green_dot.png"
    And I visually click "Departures from here" using fallback OCR text "Departures from here" or image template "src/test/resources/images/departures_from_here.png"
    Then I visually click "the motorway on the map" using OCR text " Rd"

  @web @positive1
  Scenario: Visually navigate Metro interactive map using image matching and OCR callouts - using OCR text
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually click "a station solid green dot on the map" using fallback OCR text "Rouse Hill" or image template "src/test/resources/images/solid_green_dot.png"
    And I visually click "Departures from here" using fallback OCR text "Departures from here" or image template "src/test/resources/images/departures_from_here.png"
    Then I visually click "the motorway on the map" using OCR text " Rd"

  @web @findAll @positive
  Scenario: Visually find all station dots, verify minimum count, and click by position alias
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually find all instances of "a station solid green dot on the map" using image template "src/test/resources/images/solid_green_dot.png"
    Then I verify at least 2 visual elements are present using image template "src/test/resources/images/solid_green_dot.png"
    When I visually click the "first" element matching image template "src/test/resources/images/solid_green_dot.png"
    And I visually click "Departures from here" using fallback OCR text "Departures from here" or image template "src/test/resources/images/departures_from_here.png"

  @web @proximity @positive3
  Scenario: Visually inspect and click element using spatial proximity relative to anchor text
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually click "a station solid green dot on the map" using fallback OCR text "Rouse Hill" or image template "src/test/resources/images/solid_green_dot.png"
    Then I verify visual element "Departures from here" is present using OCR text "Departures" "near" "from here"

  @web @fallback @positive
  Scenario: Visually find all instances using multi-modal fallback matching
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually find all instances of "Metro map elements" using fallback OCR text "Rouse Hill" or image template "src/test/resources/images/solid_green_dot.png"
    When I visually click the "last" element matching image template "src/test/resources/images/solid_green_dot.png"

  @web @region @positive
  Scenario: Visually verify element within region-restricted bounding box
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    Then I verify visual element "Explore the new route section heading" is present using OCR text "Explore" within region 0 0 1920 1080

  # LLM vision scenarios. Tagged @llm so NO GitHub Actions job selects them (CI runs only
  # @proximity / @region). Run locally with a configured LLM endpoint + key, e.g.:
  #   CONFIG=./configs/transportnsw_ocr_config.properties TAG="@transportnsw and @llm" ./gradlew run
  # Requires LLM_VISION_FALLBACK_ENABLED=true and the provider endpoint/key in the config/env.
  #
  # These scenarios locate the 'Select route' dropdown within the Route Map panel (a large, labelled
  # control the LLM locates reliably - pinpoint-clicking a pixel-sized map dot is the weakest case
  # for an LLM locator). The locate is region-scoped to the panel so the model cannot return a
  # coordinate up in the body text, and the outcome is VERIFIED against the DOM: the Then step asserts
  # the dropdown's options list actually opened, so a wrong locate fails the test instead of silently
  # passing a click that hit nothing.
  @web @llm @ocrThenLlm
  Scenario: Open the Route Map route selector using the OCR-then-LLM fallback strategy
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually click "the Select route dropdown" within the Route Map panel using "OCR-then-LLM" with description "the 'Select route' dropdown field showing the Metro route between Tallawong and Bankstown"
    Then the route selector options list is displayed

  @web @llm @llmOnly
  Scenario: Open the Route Map route selector using the LLM vision locator only
    Given I navigate to the Transport NSW Metro page at "https://transportnsw.info/travel-info/ways-to-get-around/metro"
    When I scroll to the "Explore the new route" interactive map section
    And I visually click "the Select route dropdown" within the Route Map panel using "LLM" with description "the 'Select route' dropdown field showing the Metro route between Tallawong and Bankstown"
    Then the route selector options list is displayed
