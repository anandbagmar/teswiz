package com.znsio.teswiz.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;

import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.SAMPLE_TEST_CONTEXT;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.runner.Driver;
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
        Driver driver = Drivers.getDriverForUser(SAMPLE_TEST_CONTEXT.ME);
        driver.getInnerDriver().manage().addCookie(new Cookie(key, value));
    }

    @Then("the cookie {string} should be present in the browser session")
    public void theCookieShouldBePresentInTheBrowserSession(String key) {
        Driver driver = Drivers.getDriverForUser(SAMPLE_TEST_CONTEXT.ME);
        Set<Cookie> cookies = driver.getInnerDriver().manage().getCookies();
        assertThat(cookies).extracting(Cookie::getName).contains(key);
    }

    @When("I delete the session cookie {string}")
    public void iDeleteTheSessionCookie(String key) {
        Driver driver = Drivers.getDriverForUser(SAMPLE_TEST_CONTEXT.ME);
        driver.getInnerDriver().manage().deleteCookieNamed(key);
    }

    @Then("the cookie {string} should not exist in the browser session")
    public void theCookieShouldNotExistInTheBrowserSession(String key) {
        Driver driver = Drivers.getDriverForUser(SAMPLE_TEST_CONTEXT.ME);
        Set<Cookie> cookies = driver.getInnerDriver().manage().getCookies();
        assertThat(cookies).extracting(Cookie::getName).doesNotContain(key);
    }

    @When("I set the window viewport size to {int} width and {int} height")
    public void iSetTheWindowViewportSizeToWidthAndHeight(int width, int height) {
        Driver driver = Drivers.getDriverForUser(SAMPLE_TEST_CONTEXT.ME);
        driver.getInnerDriver().manage().window().setSize(new Dimension(width, height));
    }

    @Then("the window viewport size should be {int} width and {int} height")
    public void theWindowViewportSizeShouldBeWidthAndHeight(int width, int height) {
        Driver driver = Drivers.getDriverForUser(SAMPLE_TEST_CONTEXT.ME);
        Dimension size = driver.getInnerDriver().manage().window().getSize();
        assertThat(size.getWidth()).isEqualTo(width);
        assertThat(size.getHeight()).isEqualTo(height);
    }

    @When("I execute an async script with {int}ms delay returning {string}")
    public void iExecuteAnAsyncScriptWithMsDelayReturning(int delay, String expectedReturn) {
        Driver driver = Drivers.getDriverForUser(SAMPLE_TEST_CONTEXT.ME);
        JavascriptExecutor executor = (JavascriptExecutor) driver.getInnerDriver();
        String script = String.format(
                "var callback = arguments[arguments.length - 1];" +
                "setTimeout(function() { callback('%s'); }, %d);", expectedReturn, delay);
        Object result = executor.executeAsyncScript(script);
        context.addTestState("asyncScriptResult", result);
    }

    @Then("the async script execution result should be {string}")
    public void theAsyncScriptExecutionResultShouldBe(String expectedReturn) {
        Object result = context.getTestState("asyncScriptResult");
        assertThat(result).isEqualTo(expectedReturn);
    }
}
