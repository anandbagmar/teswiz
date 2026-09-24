package com.znsio.teswiz.steps;

import com.znsio.teswiz.businessLayer.apiParity.ApiEngineParityBL;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ApiEngineParitySteps {
    private final ApiEngineParityBL parityBL = new ApiEngineParityBL();

    @Given("I set the API engine to {string}")
    public void setApiEngine(String engine) {
        parityBL.setEngine(engine);
    }

    @When("I send a GET request for post 1")
    public void sendGetRequest() {
        parityBL.sendGetRequest();
    }

    @When("I send a POST request to create a post")
    public void sendPostRequest() {
        parityBL.sendPostRequest();
    }

    @When("I send a PUT request to replace post 1")
    public void sendPutRequest() {
        parityBL.sendPutRequest();
    }

    @When("I send a PATCH request to modify post 1")
    public void sendPatchRequest() {
        parityBL.sendPatchRequest();
    }

    @When("I send a DELETE request for post 1")
    public void sendDeleteRequest() {
        parityBL.sendDeleteRequest();
    }

    @When("I send a HEAD request for post 1")
    public void sendHeadRequest() {
        parityBL.sendHeadRequest();
    }

    @When("I send an OPTIONS request for posts")
    public void sendOptionsRequest() {
        parityBL.sendOptionsRequest();
    }

    @When("I request an HTML webpage")
    public void sendHtmlRequest() {
        parityBL.sendHtmlRequest();
    }

    @Then("the status code should be {int}")
    public void verifyStatusCode(int expectedStatusCode) {
        parityBL.verifyStatusCode(expectedStatusCode);
    }

    @Then("the status code should be less than {int}")
    public void verifyStatusCodeLessThan(int limit) {
        parityBL.verifyStatusCodeLessThan(limit);
    }

    @Then("the response should contain HTML content")
    public void verifyHtmlContent() {
        parityBL.verifyHtmlContent();
    }
}
