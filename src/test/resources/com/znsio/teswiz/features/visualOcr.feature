@visualOcr
Feature: Scenarios for Visual OCR & Image Recognition capability

  @unit @ocr_disabled
  Scenario: Verify visual locators throw VisualSubsystemDisabledException when OCR is disabled
    Given Visual OCR capability is explicitly disabled
    Then attempting to find visual element by text "Login" should throw VisualSubsystemDisabledException
    And attempting to find visual element by image "login_icon.png" should throw VisualSubsystemDisabledException
    And attempting to find visual element by text "Login" or image "login_icon.png" should throw VisualSubsystemDisabledException
    And attempting to find visual element by image "login_icon.png" or text "Login" should throw VisualSubsystemDisabledException

#  IS_OCR_ENABLED=true CONFIG=./configs/theapp/theapp_local_web_config.properties PLATFORM=web TAG="@visualOcr and @crossPlatformOcr" ./gradlew run
#  IS_OCR_ENABLED=true CONFIG=./configs/theapp/theapp_local_android_config.properties PLATFORM=android TAG="@visualOcr and @crossPlatformOcr" ./gradlew run
#  IS_OCR_ENABLED=true CONFIG=./configs/theapp/theapp_local_ios_config.properties PLATFORM=iOS TAG="@visualOcr and @crossPlatformOcr" ./gradlew run
  @android @web @iOS @ocr @crossPlatformOcr
  Scenario: Visually inspect and verify elements using OCR across Web, Android, and iOS
    Given I start the app
    When I visually inspect "Login screen element" using OCR text "Login"
    Then I verify at least 1 visual elements are present using OCR text "Login"

