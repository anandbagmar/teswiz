package com.znsio.teswiz.steps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.businessLayer.covidtracking.CovidTrackingBL;
import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.entities.SAMPLE_TEST_CONTEXT;
import com.znsio.teswiz.runner.Drivers;
import com.znsio.teswiz.runner.Runner;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class CovidTrackingOcrSteps {
    private static final Logger LOGGER = LogManager.getLogger(CovidTrackingOcrSteps.class.getName());
    private final TestExecutionContext context;

    public CovidTrackingOcrSteps() {
        context = SessionContext.getTestExecutionContext(Thread.currentThread().getId());
    }

    @Given("I navigate to the Covid Tracking dashboard at {string}")
    public void iNavigateToTheCovidTrackingDashboardAt(String url) {
        LOGGER.info("Opening Covid Tracking dashboard: " + url);
        Drivers.createDriverFor(SAMPLE_TEST_CONTEXT.ME, "covidtracking", "chrome", Platform.web, context);
        new CovidTrackingBL(SAMPLE_TEST_CONTEXT.ME, Runner.getPlatform()).navigateToDashboard(url);
    }

    @When("I visually inspect the metrics card using OCR text {string}")
    public void iVisuallyInspectTheMetricsCardUsingOcrText(String metricName) {
        new CovidTrackingBL(SAMPLE_TEST_CONTEXT.ME, Runner.getPlatform()).inspectMetricCard(metricName);
    }

    @When("I visually select a state using OCR text {string}")
    public void iVisuallySelectAStateUsingOcrText(String stateName) {
        new CovidTrackingBL(SAMPLE_TEST_CONTEXT.ME, Runner.getPlatform()).selectState(stateName);
    }

    @Then("I visually verify the dashboard chart header using image template {string}")
    public void iVisuallyVerifyTheDashboardChartHeaderUsingImageTemplate(String imageTemplatePath) {
        new CovidTrackingBL(SAMPLE_TEST_CONTEXT.ME, Runner.getPlatform()).verifyChartHeader(imageTemplatePath);
    }
}
