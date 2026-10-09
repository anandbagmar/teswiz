package com.znsio.teswiz.web.playwright;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

class PlaywrightLocatorTest {

    @Test
    void mapsStandardSeleniumByIdToIdStrategy() {
        PlaywrightLocator locator = PlaywrightLocator.from(By.id("username"));

        assertThat(locator.strategy()).isEqualTo("id");
        assertThat(locator.value()).isEqualTo("username");
    }

    @Test
    void mapsStandardSeleniumCssSelectorToCssStrategy() {
        PlaywrightLocator locator = PlaywrightLocator.from(By.cssSelector(".login-form input"));

        assertThat(locator.strategy()).isEqualTo("css");
        assertThat(locator.value()).isEqualTo(".login-form input");
    }

    @Test
    void mapsStandardSeleniumXpathToXpathStrategy() {
        PlaywrightLocator locator = PlaywrightLocator.from(By.xpath("//button[@type='submit']"));

        assertThat(locator.strategy()).isEqualTo("xpath");
        assertThat(locator.value()).isEqualTo("//button[@type='submit']");
    }

    @Test
    void mapsStandardSeleniumPartialLinkTextToItsStrategy() {
        PlaywrightLocator locator = PlaywrightLocator.from(By.partialLinkText("Log"));

        assertThat(locator.strategy()).isEqualTo("partialLinkText");
        assertThat(locator.value()).isEqualTo("Log");
    }

    @Test
    void mapsPlaywrightNativeTextLocatorDirectlyFromItsStructuredFields() {
        PlaywrightLocator locator = PlaywrightLocator.from(PlaywrightBy.text("Allow All"));

        assertThat(locator.strategy()).isEqualTo("text");
        assertThat(locator.value()).isEqualTo("Allow All");
    }

    @Test
    void mapsPlaywrightNativeRoleLocatorPreservingRoleAndAccessibleName() {
        PlaywrightLocator locator = PlaywrightLocator.from(PlaywrightBy.role("button", "Accept cookies"));

        assertThat(locator.strategy()).isEqualTo("role");
        assertThat(locator.value()).isEqualTo("button|Accept cookies");
    }

    @Test
    void rejectsAByWhoseStringFormHasNoRecognisedStrategyPrefix() {
        By unrecognisedLocator = new By() {
            @Override
            public List<WebElement> findElements(SearchContext context) {
                return List.of();
            }

            @Override
            public String toString() {
                return "SomeVendorBy.magic: whatever";
            }
        };

        assertThatThrownBy(() -> PlaywrightLocator.from(unrecognisedLocator))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Unsupported Playwright locator");
    }
}
