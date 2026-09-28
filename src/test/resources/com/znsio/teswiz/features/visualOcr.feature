@visualOcr
Feature: Scenarios for Visual OCR & Image Recognition capability

  @unit @ocr_disabled
  Scenario: Verify visual locators throw VisualSubsystemDisabledException when OCR is disabled
    Given Visual OCR capability is explicitly disabled
    Then attempting to find visual element by text "Login" should throw VisualSubsystemDisabledException
    And attempting to find visual element by image "login_icon.png" should throw VisualSubsystemDisabledException
    And attempting to find visual element by text "Login" or image "login_icon.png" should throw VisualSubsystemDisabledException
    And attempting to find visual element by image "login_icon.png" or text "Login" should throw VisualSubsystemDisabledException
