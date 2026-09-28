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
        driver.scrollToBottom();
        return this;
    }

    @Override
    public VisualElement clickStationGreenDotOnMap(String greenDotImageTemplatePath) {
        LOGGER.info("Finding and clicking solid green dot on map using image template: " + greenDotImageTemplatePath);
        VisualElement greenDot = driver.findByImage(List.of(greenDotImageTemplatePath));
        greenDot.click();
        return greenDot;
    }

    @Override
    public VisualElement clickCalloutOptionByText(String text) {
        LOGGER.info("Finding and clicking callout option using OCR text: " + text);
        VisualElement calloutOption = driver.findByText(text);
        calloutOption.click();
        return calloutOption;
    }

    @Override
    public VisualElement clickStationNameOnMapByText(String stationName) {
        LOGGER.info("Finding and clicking station name on map using OCR text: " + stationName);
        VisualElement stationElement = driver.findByText(stationName);
        stationElement.click();
        return stationElement;
    }

    @Override
    public boolean isStationPageDisplayedFor(String stationName) {
        LOGGER.info("Verifying station page display for: " + stationName);
        String currentUrl = driver.getInnerDriver().getCurrentUrl();
        String pageTitle = driver.getInnerDriver().getTitle();
        return currentUrl.toLowerCase().contains(stationName.toLowerCase()) || pageTitle.toLowerCase().contains(stationName.toLowerCase());
    }
}
