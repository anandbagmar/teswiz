package com.znsio.teswiz.steps;

import com.znsio.teswiz.businessLayer.pwApi.PlaywrightApiBL;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

public class PlaywrightApiSteps {
    private final PlaywrightApiBL pwApiBL = new PlaywrightApiBL();

    @Given("I send a GET request using Playwright API engine")
    public void sendGetRequest() {
        pwApiBL.sendGetRequest();
    }

    @Then("the response status code should be {int}")
    public void verifyGetStatusCode(int statusCode) {
        pwApiBL.verifyStatusCode(statusCode);
    }

    @Given("I send a POST request with payload using Playwright API engine")
    public void sendPostRequest() {
        pwApiBL.sendPostRequest();
    }

    @Then("the created post status code should be {int}")
    public void verifyPostStatusCode(int statusCode) {
        pwApiBL.verifyStatusCode(statusCode);
    }
}
