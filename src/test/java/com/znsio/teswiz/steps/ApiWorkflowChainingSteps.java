package com.znsio.teswiz.steps;

import com.znsio.teswiz.businessLayer.apiChaining.ApiWorkflowChainingBL;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class ApiWorkflowChainingSteps {
    private final ApiWorkflowChainingBL workflowBL = new ApiWorkflowChainingBL();

    @When("I create a new user post via API")
    public void createPost() {
        workflowBL.createPost();
    }

    @When("I extract the created post ID from response")
    public void extractPostId() {
        workflowBL.extractCreatedPostId();
    }

    @When("I fetch details of the post using the extracted ID")
    public void fetchPostDetails() {
        workflowBL.fetchPostDetails();
    }

    @When("I update the title of the post using PUT")
    public void updatePostTitle() {
        workflowBL.updatePostTitle();
    }

    @When("I verify response schema and latency is under {long} ms")
    public void verifyLatency(long maxLatencyMs) {
        workflowBL.verifyLatency(maxLatencyMs);
    }

    @Then("I delete the post using DELETE and verify status code {int}")
    public void deletePost(int expectedStatusCode) {
        workflowBL.deletePost(expectedStatusCode);
    }
}
