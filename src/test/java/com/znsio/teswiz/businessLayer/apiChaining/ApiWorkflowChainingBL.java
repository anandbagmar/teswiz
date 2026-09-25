package com.znsio.teswiz.businessLayer.apiChaining;

import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.services.ApiService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiWorkflowChainingBL {
    private static final Logger LOGGER = LogManager.getLogger(ApiWorkflowChainingBL.class.getName());
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/posts";

    private TeswizApiResponse lastResponse;
    private int extractedPostId;

    public ApiWorkflowChainingBL setEngine(String engine) {
        LOGGER.info("Setting API_ENGINE to {}", engine);
        System.setProperty(Setup.API_ENGINE, engine);
        return this;
    }

    public ApiWorkflowChainingBL createPost() {
        LOGGER.info("Step 1: Creating a new user post via API");
        String payload = "{\"title\":\"Workflow Chaining\",\"body\":\"API user scenario chaining\",\"userId\":1}";
        lastResponse = ApiService.post(BASE_URL, payload);
        assertThat(lastResponse.getStatusCode()).isEqualTo(201);
        return this;
    }

    public ApiWorkflowChainingBL extractCreatedPostId() {
        LOGGER.info("Step 2: Extracting post ID from creation response");
        assertThat(lastResponse).isNotNull();
        extractedPostId = lastResponse.asJsonObject().getInt("id");
        LOGGER.info("Extracted post ID: {}", extractedPostId);
        assertThat(extractedPostId).isGreaterThan(0);
        return this;
    }

    public ApiWorkflowChainingBL fetchPostDetails() {
        LOGGER.info("Step 3: Fetching post details using extracted ID (target endpoint /1 for mock API)");
        String fetchUrl = BASE_URL + "/1";
        lastResponse = ApiService.get(fetchUrl);
        assertThat(lastResponse.getStatusCode()).isEqualTo(200);
        assertThat(lastResponse.asJsonObject().getInt("id")).isEqualTo(1);
        return this;
    }

    public ApiWorkflowChainingBL updatePostTitle() {
        LOGGER.info("Step 4: Updating post title via PUT method");
        String updateUrl = BASE_URL + "/1";
        String updatePayload = "{\"id\":1,\"title\":\"Chained Update Title\",\"body\":\"Updated body content\",\"userId\":1}";
        lastResponse = ApiService.put(updateUrl, updatePayload);
        assertThat(lastResponse.getStatusCode()).isEqualTo(200);
        assertThat(lastResponse.asJsonObject().getString("title")).isEqualTo("Chained Update Title");
        return this;
    }

    public ApiWorkflowChainingBL verifyLatency(long maxAllowedLatencyMs) {
        LOGGER.info("Step 5: Verifying API response latency is under {} ms", maxAllowedLatencyMs);
        assertThat(lastResponse).isNotNull();
        long responseTime = lastResponse.getResponseTimeInMs();
        LOGGER.info("Actual API response time: {} ms", responseTime);
        assertThat(responseTime)
                .as("Response latency check")
                .isLessThan(maxAllowedLatencyMs);
        return this;
    }

    public ApiWorkflowChainingBL deletePost(int expectedStatusCode) {
        LOGGER.info("Step 6: Deleting post via DELETE method");
        String deleteUrl = BASE_URL + "/1";
        lastResponse = ApiService.delete(deleteUrl);
        assertThat(lastResponse.getStatusCode()).isEqualTo(expectedStatusCode);
        return this;
    }
}
