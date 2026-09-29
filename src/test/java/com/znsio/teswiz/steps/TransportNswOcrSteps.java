package com.znsio.teswiz.steps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.businessLayer.transportnsw.TransportNswOcrBL;
import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.entities.SAMPLE_TEST_CONTEXT;
import com.znsio.teswiz.runner.Drivers;
import com.znsio.teswiz.runner.Runner;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TransportNswOcrSteps {
    private static final Logger LOGGER = LogManager.getLogger(TransportNswOcrSteps.class.getName());
    private final TestExecutionContext context;

    public TransportNswOcrSteps() {
        context = SessionContext.getTestExecutionContext(Thread.currentThread().getId());
    }

    @Given("I navigate to the Transport NSW Metro page at {string}")
    public void iNavigateToTheTransportNswMetroPageAt(String url) {
        LOGGER.info("Opening Transport NSW Metro page: " + url);
        Drivers.createDriverFor(SAMPLE_TEST_CONTEXT.ME, "transportnsw", "chrome", Platform.web, context);
        new TransportNswOcrBL(SAMPLE_TEST_CONTEXT.ME, Runner.getPlatform()).navigateToMetroPage(url);
    }

    @When("I scroll to the {string} interactive map section")
    public void iScrollToTheInteractiveMapSection(String sectionName) {
        new TransportNswOcrBL(SAMPLE_TEST_CONTEXT.ME, Runner.getPlatform()).scrollToExploreRouteMap();
    }



    @Then("I verify the station departures page for {string} is displayed")
    public void iVerifyTheStationDeparturesPageForIsDisplayed(String stationName) {
        new TransportNswOcrBL(SAMPLE_TEST_CONTEXT.ME, Runner.getPlatform()).verifyStationDetailsDisplayed(stationName);
    }
}
