package com.znsio.teswiz.businessLayer.restUser;

import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.businessLayer.weatherAPI.WeatherAPIBL;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.services.ApiService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonPlaceHolderBL {

    private static final Logger LOGGER = LogManager.getLogger(WeatherAPIBL.class.getName());
    private final Map<String, Object> testData = Runner.getTestDataAsMap("RestUser_API");
    private final String base_URL = testData.get("url").toString();

    public JsonPlaceHolderBL createPost() {
        Object jsonBody = testData.get("postBody");
        LOGGER.info("Creating a post");
        TeswizApiResponse response = ApiService.post(base_URL, jsonBody);
        assertThat(response.getStatusCode()).as("Received API status code for POST method incorrect!")
                .isEqualTo(201);
        LOGGER.info("Verifying post is created successfully");
        assertThat(response.asJsonObject().getInt("id")).as("API status code for POST method incorrect!")
                .isEqualTo(101);
        return this;
    }

    public JSONObject updatePost() {
        LOGGER.info("Updating a post");
        Object jsonBody = testData.get("patchBody");
        TeswizApiResponse response = ApiService.patch(base_URL + "/1", jsonBody);
        assertThat(response.getStatusCode()).as("Received API status code for PATCH method incorrect!")
                .isEqualTo(200);
        return response.asJsonObject();
    }

    public JsonPlaceHolderBL verifyPostUpdatedSuccessfully(JSONObject jsonResponse) {
        LOGGER.info("Verifying post is updated successfully");
        String updatedTitle = testData.get("updatedTitle").toString();
        assertThat(jsonResponse.get("title")).as("Updated Title not matched!")
                .isEqualTo(updatedTitle);
        return this;
    }

    public int deletePost() {
        LOGGER.info("Verifying post is deleted successfully");
        TeswizApiResponse response = ApiService.delete(base_URL + "/1");
        assertThat(response.getStatusCode()).as("Received API status code for Delete method incorrect!")
                .isEqualTo(200);
        return response.getStatusCode();
    }

    public JsonPlaceHolderBL verifyIfPostDeleted(int status) {
        assertThat(status).as("Received API status code for Delete method incorrect!")
                .isEqualTo(200);
        return this;
    }
}
