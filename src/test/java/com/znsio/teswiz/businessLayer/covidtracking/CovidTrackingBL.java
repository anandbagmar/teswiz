package com.znsio.teswiz.businessLayer.covidtracking;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.covidtracking.CovidTrackingScreen;

public class CovidTrackingBL {
    private static final Logger LOGGER = LogManager.getLogger(CovidTrackingBL.class.getName());
    private final TestExecutionContext context;
    private final String currentUserPersona;
    private final Platform currentPlatform;

    public CovidTrackingBL(String userPersona, Platform forPlatform) {
        long threadId = Thread.currentThread().getId();
        this.context = Runner.getTestExecutionContext(threadId);
        this.currentUserPersona = userPersona;
        this.currentPlatform = forPlatform;
        Runner.setCurrentDriverForUser(userPersona, forPlatform, context);
    }

    public CovidTrackingBL navigateToDashboard(String url) {
        LOGGER.info("Navigating to dashboard: " + url);
        CovidTrackingScreen.get().navigateTo(url);
        return this;
    }

    public CovidTrackingBL inspectMetricCard(String metricName) {
        LOGGER.info("Inspecting metric card visually by OCR text: " + metricName);
        VisualElement metricElement = CovidTrackingScreen.get().findMetricByOcrText(metricName);
        assertThat(metricElement).as("Visual metric element '" + metricName + "' should be found").isNotNull();
        return this;
    }

    public CovidTrackingBL selectState(String stateName) {
        LOGGER.info("Selecting state visually by OCR text: " + stateName);
        VisualElement stateElement = CovidTrackingScreen.get().selectStateByOcrText(stateName);
        assertThat(stateElement).as("State element '" + stateName + "' should be found and clicked").isNotNull();
        return this;
    }

    public CovidTrackingBL verifyChartHeader(String imageTemplatePath) {
        LOGGER.info("Verifying chart header visually by image template: " + imageTemplatePath);
        VisualElement chartElement = CovidTrackingScreen.get().findChartHeaderByImageTemplate(imageTemplatePath);
        assertThat(chartElement).as("Chart header image element should be found").isNotNull();
        return this;
    }
}
