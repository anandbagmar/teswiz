package com.znsio.teswiz.screen.web.playwrightjava.transportnsw;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.znsio.teswiz.entities.VisionStrategy;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.transportnsw.TransportNswOcrScreen;
import com.znsio.teswiz.web.playwright.screen.PlaywrightJavaScreenContext;

public class TransportNswOcrScreenPlaywrightJava extends TransportNswOcrScreen {
    private static final String SCREEN_NAME = TransportNswOcrScreenPlaywrightJava.class.getSimpleName();
    private static final Logger LOGGER = LogManager.getLogger(SCREEN_NAME);

    private final PlaywrightJavaScreenContext context;
    private final Visual visually;

    public TransportNswOcrScreenPlaywrightJava(PlaywrightJavaScreenContext context) {
        this.context = context;
        this.visually = context.visual();
        LOGGER.info("Initialized " + SCREEN_NAME + " for Playwright Java");
    }

    @Override
    public TransportNswOcrScreen navigateTo(String url) {
        LOGGER.info("Playwright Java - Navigating to Transport NSW Metro page: " + url);
        context.page().navigate(url);
        return this;
    }

    @Override
    public TransportNswOcrScreen scrollToExploreRouteMap() {
        LOGGER.info("Playwright Java - Scrolling down to 'Explore the new route' interactive map section");
        Locator exploreSection = context.page().getByText("Explore the new route",
                new Page.GetByTextOptions().setExact(true));
        exploreSection.first().scrollIntoViewIfNeeded();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        visually.checkWindow(SCREEN_NAME, "'Explore the new route' map section");
        return this;
    }

    @Override
    public VisualElement clickElementByImage(String elementName, String imagePath) {
        LOGGER.info("Playwright Java - Finding and clicking '{}' using image template: {}", elementName, imagePath);
        VisualElement visualElement = visually.vision(VisionStrategy.OCR).findByImage(List.of(imagePath));
        visually.checkWindow(SCREEN_NAME, "Before clicking: " + elementName);
        visualElement.click();
        visually.checkWindow(SCREEN_NAME, "After clicking: " + elementName);
        return visualElement;
    }

    @Override
    public VisualElement clickElementByOcrText(String elementName, String ocrText) {
        LOGGER.info("Playwright Java - Finding and clicking '{}' using OCR text: {}", elementName, ocrText);
        VisualElement stationElement = visually.vision(VisionStrategy.OCR).findByText(ocrText);
        visually.checkWindow(SCREEN_NAME, "Before clicking: " + elementName);
        stationElement.click();
        visually.checkWindow(SCREEN_NAME, "After clicking: " + elementName);
        return stationElement;
    }

    @Override
    public VisualElement clickElementByOcrThenLlm(String elementName, String description) {
        LOGGER.info("Playwright Java - Finding and clicking '{}' using OCR_THEN_LLM with description: {}", elementName, description);
        VisualElement element = visually.vision(VisionStrategy.OCR_THEN_LLM).findByText(description);
        visually.checkWindow(SCREEN_NAME, "Before clicking (OCR_THEN_LLM): " + elementName);
        element.click();
        visually.checkWindow(SCREEN_NAME, "After clicking (OCR_THEN_LLM): " + elementName);
        return element;
    }

    @Override
    public VisualElement clickElementByLlm(String elementName, String description) {
        LOGGER.info("Playwright Java - Finding and clicking '{}' using LLM with description: {}", elementName, description);
        VisualElement element = visually.vision(VisionStrategy.LLM).findByText(description);
        visually.checkWindow(SCREEN_NAME, "Before clicking (LLM): " + elementName);
        element.click();
        visually.checkWindow(SCREEN_NAME, "After clicking (LLM): " + elementName);
        return element;
    }

    @Override
    public VisualElement clickCalloutOptionByText(String text) {
        LOGGER.info("Playwright Java - Finding and clicking callout option using OCR text: " + text);
        VisualElement calloutOption = visually.vision(VisionStrategy.OCR).findByText(text);
        visually.checkWindow(SCREEN_NAME, "Callout popup option: " + text);
        calloutOption.click();
        visually.checkWindow(SCREEN_NAME, "Clicked callout option: " + text);
        return calloutOption;
    }

    @Override
    public VisualElement clickStationNameOnMapByText(String stationName) {
        LOGGER.info("Playwright Java - Finding and clicking station name on map using OCR text: " + stationName);
        VisualElement stationElement = visually.vision(VisionStrategy.OCR).findByText(stationName);
        visually.checkWindow(SCREEN_NAME, "Station map view: " + stationName);
        stationElement.click();
        visually.checkWindow(SCREEN_NAME, "Clicked station name on map: " + stationName);
        return stationElement;
    }

    @Override
    public boolean isStationPageDisplayedFor(String stationName) {
        LOGGER.info("Playwright Java - Verifying station page display for: " + stationName);
        Locator stationText = context.page().getByText(stationName, new Page.GetByTextOptions().setExact(true));
        return stationText.count() > 0;
    }
}
