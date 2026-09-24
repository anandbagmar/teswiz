package com.znsio.teswiz.businessLayer.pwApi;

import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.services.ApiService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;

public class PlaywrightApiBL {
    private static final Logger LOGGER = LogManager.getLogger(PlaywrightApiBL.class.getName());
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/posts";

    private TeswizApiResponse lastResponse;

    public PlaywrightApiBL sendGetRequest() {
        LOGGER.info("Sending GET request to {}", BASE_URL + "/1");
        lastResponse = ApiService.get(BASE_URL + "/1");
        return this;
    }

    public PlaywrightApiBL sendPostRequest() {
        LOGGER.info("Sending POST request to {}", BASE_URL);
        String jsonPayload = "{\"title\":\"playwright api\",\"body\":\"testing pw api engine\",\"userId\":1}";
        lastResponse = ApiService.post(BASE_URL, jsonPayload);
        return this;
    }

    public PlaywrightApiBL verifyStatusCode(int expectedStatusCode) {
        LOGGER.info("Verifying status code is {}", expectedStatusCode);
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatusCode())
                .as("API Status Code check")
                .isEqualTo(expectedStatusCode);
        return this;
    }
}
