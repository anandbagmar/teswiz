package com.znsio.teswiz.businessLayer.apiParity;

import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.services.ApiService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiEngineParityBL {
    private static final Logger LOGGER = LogManager.getLogger(ApiEngineParityBL.class.getName());
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com/posts";
    private static final String HTML_URL = "https://httpbin.org/html";

    private TeswizApiResponse lastResponse;

    public ApiEngineParityBL setEngine(String engine) {
        LOGGER.info("Setting API_ENGINE to {}", engine);
        System.setProperty(Setup.API_ENGINE, engine);
        return this;
    }

    public ApiEngineParityBL sendGetRequest() {
        LOGGER.info("Sending GET request to {}", BASE_URL + "/1");
        lastResponse = ApiService.get(BASE_URL + "/1");
        return this;
    }

    public ApiEngineParityBL sendPostRequest() {
        LOGGER.info("Sending POST request to {}", BASE_URL);
        String jsonPayload = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";
        lastResponse = ApiService.post(BASE_URL, jsonPayload);
        return this;
    }

    public ApiEngineParityBL sendPutRequest() {
        LOGGER.info("Sending PUT request to {}", BASE_URL + "/1");
        String jsonPayload = "{\"id\":1,\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";
        lastResponse = ApiService.put(BASE_URL + "/1", jsonPayload);
        return this;
    }

    public ApiEngineParityBL sendPatchRequest() {
        LOGGER.info("Sending PATCH request to {}", BASE_URL + "/1");
        String jsonPayload = "{\"title\":\"updated title\"}";
        lastResponse = ApiService.patch(BASE_URL + "/1", jsonPayload);
        return this;
    }

    public ApiEngineParityBL sendDeleteRequest() {
        LOGGER.info("Sending DELETE request to {}", BASE_URL + "/1");
        lastResponse = ApiService.delete(BASE_URL + "/1");
        return this;
    }

    public ApiEngineParityBL sendHeadRequest() {
        LOGGER.info("Sending HEAD request to {}", BASE_URL + "/1");
        lastResponse = ApiService.head(BASE_URL + "/1");
        return this;
    }

    public ApiEngineParityBL sendOptionsRequest() {
        LOGGER.info("Sending OPTIONS request to {}", BASE_URL);
        lastResponse = ApiService.options(BASE_URL);
        return this;
    }

    public ApiEngineParityBL sendHtmlRequest() {
        LOGGER.info("Sending GET request for HTML content to {}", HTML_URL);
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html");
        lastResponse = ApiService.get(HTML_URL, null, headers);
        return this;
    }

    public ApiEngineParityBL verifyStatusCode(int expectedStatusCode) {
        LOGGER.info("Verifying status code is {}", expectedStatusCode);
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatusCode())
                .as("Status code check")
                .isEqualTo(expectedStatusCode);
        return this;
    }

    public ApiEngineParityBL verifyStatusCodeLessThan(int limit) {
        LOGGER.info("Verifying status code is less than {}", limit);
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatusCode())
                .as("Status code limit check")
                .isLessThan(limit);
        return this;
    }

    public ApiEngineParityBL verifyHtmlContent() {
        LOGGER.info("Verifying response contains HTML content");
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatusCode()).isEqualTo(200);
        String body = lastResponse.getResponseBody();
        assertThat(body)
                .as("HTML body check")
                .containsIgnoringCase("<!DOCTYPE html>")
                .containsIgnoringCase("<html");
        return this;
    }
}
