@google-search
Feature: Scenarios for "google-search"

#  WEB_ENGINE=selenium CONFIG=./configs/googlesearch/googlesearch_local_web_config.properties PLATFORM=web TAG="@google-search and @web" ./gradlew run
#  WEB_ENGINE=playwright-java CONFIG=./configs/googlesearch/googlesearch_local_web_config.properties PLATFORM=web TAG="@google-search and @web" ./gradlew run
#  WEB_ENGINE=playwright-ts CONFIG=./configs/googlesearch/googlesearch_local_web_config.properties PLATFORM=web TAG="@google-search and @web" ./gradlew run

  @android-chrome @android
  Scenario: Google search results in local emulator using appium
    Given I search for "india" in "chrome-android"

  @browserstack @android-chrome @web @selenium @playwright-java @playwright-ts
  Scenario: Google search results across all web engines
    Given I search for "india" in "chrome-web"
