package com.znsio.teswiz.businessLayer.transportnsw;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.entities.VisionStrategy;
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

    public TransportNswOcrBL clickElementUsingOcrThenLlm(String elementName, String description) {
        LOGGER.info("Visually clicking element '{}' using OCR_THEN_LLM with description: {}", elementName, description);
        VisualElement visualElement = TransportNswOcrScreen.get().clickElementByOcrThenLlm(elementName, description);
        assertThat(visualElement).as("Visual element '" + elementName + "' should be found (OCR_THEN_LLM) and clicked").isNotNull();
        return this;
    }

    public TransportNswOcrBL clickElementUsingLlm(String elementName, String description) {
        LOGGER.info("Visually clicking element '{}' using LLM with description: {}", elementName, description);
        VisualElement visualElement = TransportNswOcrScreen.get().clickElementByLlm(elementName, description);
        assertThat(visualElement).as("Visual element '" + elementName + "' should be located by the LLM and clicked").isNotNull();
        return this;
    }

    public TransportNswOcrBL clickElementInRouteMapPanel(String elementName, VisionStrategy strategy, String description) {
        LOGGER.info("Visually clicking element '{}' within the Route Map panel using {} with description: {}",
                elementName, strategy, description);
        VisualElement visualElement = TransportNswOcrScreen.get()
                .clickElementInRouteMapPanel(elementName, strategy, description);
        assertThat(visualElement)
                .as("Visual element '" + elementName + "' should be located (" + strategy + ") within the Route Map panel and clicked")
                .isNotNull();
        return this;
    }

    public TransportNswOcrBL verifyRouteSelectorOptionsListDisplayed() {
        LOGGER.info("Verifying the route selector options list is displayed after the click");
        boolean isDisplayed = TransportNswOcrScreen.get().isRouteSelectorOptionsListDisplayed();
        assertThat(isDisplayed)
                .as("The 'Select route' dropdown options list should be displayed - i.e. the visual click actually "
                        + "landed on and opened the dropdown (a mis-located click would not open it)")
                .isTrue();
        return this;
    }

    public TransportNswOcrBL verifyStationDetailsDisplayed(String stationName) {
        LOGGER.info("Verifying station details page for: " + stationName);
        boolean isDisplayed = TransportNswOcrScreen.get().isStationPageDisplayedFor(stationName);
        assertThat(isDisplayed).as("Station page details for '" + stationName + "' should be displayed").isTrue();
        return this;
    }
}
