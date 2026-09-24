package com.znsio.teswiz.steps;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.businessLayer.parity.WebEngineParityBL;
import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.SAMPLE_TEST_CONTEXT;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.runner.Drivers;
import com.znsio.teswiz.runner.Runner;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class WebEngineParitySteps {
    private static final Logger LOGGER = LogManager.getLogger(WebEngineParitySteps.class.getName());
    private final TestExecutionContext context;

    public WebEngineParitySteps() {
        context = SessionContext.getTestExecutionContext(Thread.currentThread().getId());
        LOGGER.info("context: " + context.getTestName());
    }

    @Given("I start the web session for parity testing")
    public void iStartTheWebSessionForParityTesting() {
        context.addTestState(TEST_CONTEXT.UPDATED_BROWSER_CONFIG_FILE_FOR_THIS_TEST, "./configs/browser_config.json");
        context.addTestState(TEST_CONTEXT.UPDATED_BASE_URL_FOR_WEB, "BASE_URL");
        Drivers.createDriverFor(SAMPLE_TEST_CONTEXT.ME, Runner.getPlatform(), context);
    }

    @When("I add a session cookie {string} with value {string}")
    public void iAddASessionCookieWithValue(String key, String value) {
        new WebEngineParityBL().addSessionCookie(key, value);
    }

    @Then("the cookie {string} should be present in the browser session")
    public void theCookieShouldBePresentInTheBrowserSession(String key) {
        new WebEngineParityBL().verifyCookiePresent(key);
    }

    @When("I delete the session cookie {string}")
    public void iDeleteTheSessionCookie(String key) {
        new WebEngineParityBL().deleteSessionCookie(key);
    }

    @Then("the cookie {string} should not exist in the browser session")
    public void theCookieShouldNotExistInTheBrowserSession(String key) {
        new WebEngineParityBL().verifyCookieNotPresent(key);
    }

    @When("I set the window viewport size to {int} width and {int} height")
    public void iSetTheWindowViewportSizeToWidthAndHeight(int width, int height) {
        new WebEngineParityBL().setViewportSize(width, height);
    }

    @Then("the window viewport size should be {int} width and {int} height")
    public void theWindowViewportSizeShouldBeWidthAndHeight(int width, int height) {
        new WebEngineParityBL().verifyViewportSize(width, height);
    }

    @When("I execute an async script with {int}ms delay returning {string}")
    public void iExecuteAnAsyncScriptWithMsDelayReturning(int delay, String expectedReturn) {
        new WebEngineParityBL().executeAsyncScript(delay, expectedReturn);
    }

    @Then("the async script execution result should be {string}")
    public void theAsyncScriptExecutionResultShouldBe(String expectedReturn) {
        new WebEngineParityBL().verifyAsyncScriptResult(expectedReturn);
    }
}
