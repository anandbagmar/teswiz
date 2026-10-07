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

    public TransportNswOcrBL clickElementUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info("Visually clicking element '{}' using image template: {}", elementName, imageTemplatePath);
        VisualElement visualElement = TransportNswOcrScreen.get().clickElementByImage(elementName, imageTemplatePath);
        assertThat(visualElement).as("Visual element '" + elementName + "' should be found and clicked").isNotNull();
        return this;
    }

    public TransportNswOcrBL clickElementUsingOcrText(String elementName, String ocrText) {
        LOGGER.info("Visually clicking element '{}' using OCR text: {}", elementName, ocrText);
        VisualElement visualElement = TransportNswOcrScreen.get().clickElementByOcrText(elementName, ocrText);
        assertThat(visualElement).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found and clicked")
                .isNotNull();
        return this;
    }

    public TransportNswOcrBL verifyStationDetailsDisplayed(String stationName) {
        LOGGER.info("Verifying station details page for: " + stationName);
        boolean isDisplayed = TransportNswOcrScreen.get().isStationPageDisplayedFor(stationName);
        assertThat(isDisplayed).as("Station page details for '" + stationName + "' should be displayed").isTrue();
        return this;
    }
}
