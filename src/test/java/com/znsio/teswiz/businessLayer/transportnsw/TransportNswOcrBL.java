package com.znsio.teswiz.businessLayer.transportnsw;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.transportnsw.TransportNswOcrScreen;

public class TransportNswOcrBL {
    private static final Logger LOGGER = LogManager.getLogger(TransportNswOcrBL.class.getName());
    private final TestExecutionContext context;
    private final String currentUserPersona;
    private final Platform currentPlatform;

    public TransportNswOcrBL(String userPersona, Platform forPlatform) {
        long threadId = Thread.currentThread().getId();
        this.context = Runner.getTestExecutionContext(threadId);
        this.currentUserPersona = userPersona;
        this.currentPlatform = forPlatform;
        Runner.setCurrentDriverForUser(userPersona, forPlatform, context);
    }

    public TransportNswOcrBL navigateToMetroPage(String url) {
        LOGGER.info("Navigating to Transport NSW Metro page: " + url);
        TransportNswOcrScreen.get().navigateTo(url);
        return this;
    }

    public TransportNswOcrBL scrollToExploreRouteMap() {
        LOGGER.info("Scrolling down to 'Explore the new route' map section");
        TransportNswOcrScreen.get().scrollToExploreRouteMap();
        return this;
    }

    public TransportNswOcrBL clickSolidGreenDotOnMap(String greenDotImageTemplatePath) {
        LOGGER.info("Visually clicking solid green dot on map using image template: " + greenDotImageTemplatePath);
        VisualElement greenDot = TransportNswOcrScreen.get().clickStationGreenDotOnMap(greenDotImageTemplatePath);
        assertThat(greenDot).as("Solid green dot visual element should be found and clicked").isNotNull();
        return this;
    }

    public TransportNswOcrBL clickCalloutOption(String optionText) {
        LOGGER.info("Visually clicking callout option by OCR text: " + optionText);
        VisualElement optionElement = TransportNswOcrScreen.get().clickCalloutOptionByText(optionText);
        assertThat(optionElement).as("Callout option '" + optionText + "' should be found and clicked").isNotNull();
        return this;
    }

    public TransportNswOcrBL clickStationNameOnMap(String stationName) {
        LOGGER.info("Visually clicking station name on map by OCR text: " + stationName);
        VisualElement stationElement = TransportNswOcrScreen.get().clickStationNameOnMapByText(stationName);
        assertThat(stationElement).as("Station name element '" + stationName + "' should be found and clicked").isNotNull();
        return this;
    }

    public TransportNswOcrBL verifyStationDetailsDisplayed(String stationName) {
        LOGGER.info("Verifying station details page for: " + stationName);
        boolean isDisplayed = TransportNswOcrScreen.get().isStationPageDisplayedFor(stationName);
        assertThat(isDisplayed).as("Station page details for '" + stationName + "' should be displayed").isTrue();
        return this;
    }
}
