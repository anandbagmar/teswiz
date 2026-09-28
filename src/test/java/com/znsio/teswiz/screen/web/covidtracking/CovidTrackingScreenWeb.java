package com.znsio.teswiz.screen.web.covidtracking;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.covidtracking.CovidTrackingScreen;

public class CovidTrackingScreenWeb extends CovidTrackingScreen {
    private static final String SCREEN_NAME = CovidTrackingScreenWeb.class.getSimpleName();
    private static final Logger LOGGER = LogManager.getLogger(SCREEN_NAME);

    private final Driver driver;
    private final Visual visually;

    public CovidTrackingScreenWeb(Driver driver, Visual visually) {
        this.driver = driver;
        this.visually = visually;
        LOGGER.info("Initialized " + SCREEN_NAME);
    }

    @Override
    public CovidTrackingScreen navigateTo(String url) {
        LOGGER.info("Navigating to COVID tracking dashboard URL: " + url);
        driver.getInnerDriver().get(url);
        return this;
    }

    @Override
    public VisualElement findMetricByOcrText(String metricName) {
        LOGGER.info("Visually finding metric using OCR text: " + metricName);
        return driver.findByText(metricName);
    }

    @Override
    public VisualElement selectStateByOcrText(String stateName) {
        LOGGER.info("Visually finding and clicking state using OCR text: " + stateName);
        VisualElement stateElement = driver.findByText(stateName);
        stateElement.click();
        return stateElement;
    }

    @Override
    public VisualElement findChartHeaderByImageTemplate(String imageTemplatePath) {
        LOGGER.info("Visually finding chart element using image template: " + imageTemplatePath);
        return driver.findByImage(List.of(imageTemplatePath));
    }
}
