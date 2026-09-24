package com.znsio.teswiz.services;

import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.runner.Setup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiServiceTest {

    @BeforeEach
    void setUp() {
        System.clearProperty(Setup.API_ENGINE);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(Setup.API_ENGINE);
        com.znsio.teswiz.api.PlaywrightApiManager.closeContextForCurrentThread();
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest-assured", "playwright-java"})
    void executeGetCallWithEngine(String apiEngine) {
        System.setProperty(Setup.API_ENGINE, apiEngine);

        String sampleUrl = "https://jsonplaceholder.typicode.com/posts/1";
        TeswizApiResponse response = ApiService.get(sampleUrl);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getResponseBody()).contains("userId");
        assertThat(response.asJsonObject().getInt("id")).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest-assured", "playwright-java"})
    void executeGetCallWithQueryParamsWithEngine(String apiEngine) {
        System.setProperty(Setup.API_ENGINE, apiEngine);

        String sampleUrl = "https://jsonplaceholder.typicode.com/comments";
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("postId", 1);

        TeswizApiResponse response = ApiService.get(sampleUrl, queryParams);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.asJsonArray().length()).isGreaterThan(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest-assured", "playwright-java"})
    void executePostCallWithEngine(String apiEngine) {
        System.setProperty(Setup.API_ENGINE, apiEngine);

        String sampleUrl = "https://jsonplaceholder.typicode.com/posts";
        String postBody = "{\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";

        TeswizApiResponse response = ApiService.post(sampleUrl, postBody);

        assertThat(response.getStatusCode()).isEqualTo(201);
        assertThat(response.asJsonObject().getInt("id")).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest-assured", "playwright-java"})
    void executePutCallWithEngine(String apiEngine) {
        System.setProperty(Setup.API_ENGINE, apiEngine);

        String sampleUrl = "https://jsonplaceholder.typicode.com/posts/1";
        String putBody = "{\"id\":1,\"title\":\"foo\",\"body\":\"bar\",\"userId\":1}";

        TeswizApiResponse response = ApiService.put(sampleUrl, putBody);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.asJsonObject().getString("title")).isEqualTo("foo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest-assured", "playwright-java"})
    void executePatchCallWithEngine(String apiEngine) {
        System.setProperty(Setup.API_ENGINE, apiEngine);

        String sampleUrl = "https://jsonplaceholder.typicode.com/posts/1";
        String patchBody = "{\"title\":\"updated title\"}";

        TeswizApiResponse response = ApiService.patch(sampleUrl, patchBody);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.asJsonObject().getString("title")).isEqualTo("updated title");
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest-assured", "playwright-java"})
    void executeDeleteCallWithEngine(String apiEngine) {
        System.setProperty(Setup.API_ENGINE, apiEngine);

        String sampleUrl = "https://jsonplaceholder.typicode.com/posts/1";

        TeswizApiResponse response = ApiService.delete(sampleUrl);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest-assured", "playwright-java"})
    void executeHeadCallWithEngine(String apiEngine) {
        System.setProperty(Setup.API_ENGINE, apiEngine);

        String sampleUrl = "https://jsonplaceholder.typicode.com/posts/1";

        TeswizApiResponse response = ApiService.head(sampleUrl);

        assertThat(response.getStatusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rest-assured", "playwright-java"})
    void executeOptionsCallWithEngine(String apiEngine) {
        System.setProperty(Setup.API_ENGINE, apiEngine);

        String sampleUrl = "https://jsonplaceholder.typicode.com/posts";

        TeswizApiResponse response = ApiService.options(sampleUrl);

        assertThat(response.getStatusCode()).isLessThan(400);
    }
}
