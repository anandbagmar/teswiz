package com.znsio.teswiz.screen.web.transportnsw;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.transportnsw.TransportNswOcrScreen;

public class TransportNswOcrScreenWeb extends TransportNswOcrScreen {
    private static final String SCREEN_NAME = TransportNswOcrScreenWeb.class.getSimpleName();
    private static final Logger LOGGER = LogManager.getLogger(SCREEN_NAME);

    private final Driver driver;
    private final Visual visually;

    public TransportNswOcrScreenWeb(Driver driver, Visual visually) {
        this.driver = driver;
        this.visually = visually;
        LOGGER.info("Initialized " + SCREEN_NAME);
    }

    @Override
    public TransportNswOcrScreen navigateTo(String url) {
        LOGGER.info("Navigating to Transport NSW Metro page: " + url);
        driver.getInnerDriver().get(url);
        return this;
    }

    @Override
    public TransportNswOcrScreen scrollToExploreRouteMap() {
        LOGGER.info("Scrolling down to 'Explore the new route' interactive map section");
        By exploreSectionLocator = By.xpath("//*[contains(text(), 'Explore the new route')]");
        if (driver.isElementPresent(exploreSectionLocator)) {
            org.openqa.selenium.WebElement element = driver.findElement(exploreSectionLocator);
            ((org.openqa.selenium.JavascriptExecutor) driver.getInnerDriver())
                    .executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
        } else {
            LOGGER.warn("'Explore the new route' text element not found; performing moderate window scroll");
            ((org.openqa.selenium.JavascriptExecutor) driver.getInnerDriver())
                    .executeScript("window.scrollBy(0, 400);");
        }
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        visually.checkWindow(SCREEN_NAME, "'Explore the new route' map section");
        return this;
    }

    @Override
    public VisualElement clickElementByImage(String elementName, String imagePath) {
        LOGGER.info("Finding and clicking '{}' using image template: {}", elementName, imagePath);
        VisualElement visualElement = driver.findByImage(List.of(imagePath));
        visually.checkWindow(SCREEN_NAME, "Before clicking: " + elementName);
        if (visualElement != null) {
            visualElement.click();
        }
        visually.checkWindow(SCREEN_NAME, "After clicking: " + elementName);
        return visualElement;
    }

    @Override
    public VisualElement clickElementByOcrText(String elementName, String ocrText) {
        LOGGER.info("Finding and clicking '{}' using OCR text: {}", elementName, ocrText);
        VisualElement stationElement = driver.findByText(ocrText);
        visually.checkWindow(SCREEN_NAME, "Before clicking: " + elementName);
        if (stationElement != null) {
            stationElement.click();
        }
        visually.checkWindow(SCREEN_NAME, "After clicking: " + elementName);
        return stationElement;
    }

    @Override
    public VisualElement clickCalloutOptionByText(String text) {
        LOGGER.info("Finding and clicking callout option using OCR text: " + text);
        VisualElement calloutOption = driver.findByText(text);
        visually.checkWindow(SCREEN_NAME, "Callout popup option: " + text);
        if (calloutOption != null) {
            calloutOption.click();
        }
        visually.checkWindow(SCREEN_NAME, "Clicked callout option: " + text);
        return calloutOption;
    }

    @Override
    public VisualElement clickStationNameOnMapByText(String stationName) {
        LOGGER.info("Finding and clicking station name on map using OCR text: " + stationName);
        VisualElement stationElement = driver.findByText(stationName);
        visually.checkWindow(SCREEN_NAME, "Station map view: " + stationName);
        if (stationElement != null) {
            stationElement.click();
        }
        visually.checkWindow(SCREEN_NAME, "Clicked station name on map: " + stationName);
        return stationElement;
    }

    @Override
    public boolean isStationPageDisplayedFor(String stationName) {
        LOGGER.info("Verifying station page display for: " + stationName);
        String currentUrl = driver.getInnerDriver().getCurrentUrl();
        String pageTitle = driver.getInnerDriver().getTitle();
        if (currentUrl.toLowerCase().contains(stationName.toLowerCase())
                || pageTitle.toLowerCase().contains(stationName.toLowerCase())) {
            return true;
        }
        By stationTextLocator = By.xpath("//*[contains(translate(text(), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '" + stationName.toLowerCase() + "')]");
        return driver.isElementPresent(stationTextLocator);
    }
}
