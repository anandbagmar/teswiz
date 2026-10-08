package com.znsio.teswiz.screen.web.playwrightjava.transportnsw;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.transportnsw.TransportNswOcrScreen;

import com.znsio.teswiz.web.playwright.PlaywrightBy;

public class TransportNswOcrScreenPlaywrightJava extends TransportNswOcrScreen {
    private static final String SCREEN_NAME = TransportNswOcrScreenPlaywrightJava.class.getSimpleName();
    private static final Logger LOGGER = LogManager.getLogger(SCREEN_NAME);

    private final Driver driver;
    private final Visual visually;

    public TransportNswOcrScreenPlaywrightJava(Driver driver, Visual visually) {
        this.driver = driver;
        this.visually = visually;
        LOGGER.info("Initialized " + SCREEN_NAME + " for Playwright Java");
    }

    @Override
    public TransportNswOcrScreen navigateTo(String url) {
        LOGGER.info("Playwright Java - Navigating to Transport NSW Metro page: " + url);
        driver.getInnerDriver().get(url);
        return this;
    }

    @Override
    public TransportNswOcrScreen scrollToExploreRouteMap() {
        LOGGER.info("Playwright Java - Scrolling down to 'Explore the new route' interactive map section");
        By exploreSectionLocator = PlaywrightBy.text("Explore the new route");
        WebElement element = driver.findElement(exploreSectionLocator);
        ((org.openqa.selenium.JavascriptExecutor) driver.getInnerDriver())
                .executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        visually.checkWindow(SCREEN_NAME, "'Explore the new route' map section");
        return this;
    }

    @Override
    public VisualElement clickElementByImage(String elementName, String imagePath) {
        LOGGER.info("Playwright Java - Finding and clicking '{}' using image template: {}", elementName, imagePath);
        VisualElement visualElement = visually.findByImage(List.of(imagePath));
        visually.checkWindow(SCREEN_NAME, "Before clicking: " + elementName);
        visualElement.click();
        visually.checkWindow(SCREEN_NAME, "After clicking: " + elementName);
        return visualElement;
    }

    @Override
    public VisualElement clickElementByOcrText(String elementName, String ocrText) {
        LOGGER.info("Playwright Java - Finding and clicking '{}' using OCR text: {}", elementName, ocrText);
        VisualElement stationElement = visually.findByText(ocrText);
        visually.checkWindow(SCREEN_NAME, "Before clicking: " + elementName);
        stationElement.click();
        visually.checkWindow(SCREEN_NAME, "After clicking: " + elementName);
        return stationElement;
    }

    @Override
    public VisualElement clickCalloutOptionByText(String text) {
        LOGGER.info("Playwright Java - Finding and clicking callout option using OCR text: " + text);
        VisualElement calloutOption = visually.findByText(text);
        visually.checkWindow(SCREEN_NAME, "Callout popup option: " + text);
        calloutOption.click();
        visually.checkWindow(SCREEN_NAME, "Clicked callout option: " + text);
        return calloutOption;
    }

    @Override
    public VisualElement clickStationNameOnMapByText(String stationName) {
        LOGGER.info("Playwright Java - Finding and clicking station name on map using OCR text: " + stationName);
        VisualElement stationElement = visually.findByText(stationName);
        visually.checkWindow(SCREEN_NAME, "Station map view: " + stationName);
        stationElement.click();
        visually.checkWindow(SCREEN_NAME, "Clicked station name on map: " + stationName);
        return stationElement;
    }

    @Override
    public boolean isStationPageDisplayedFor(String stationName) {
        LOGGER.info("Playwright Java - Verifying station page display for: " + stationName);
        By stationTextLocator = PlaywrightBy.text(stationName);
        return driver.isElementPresent(stationTextLocator);
    }
}
