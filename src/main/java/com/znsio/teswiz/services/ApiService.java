package com.znsio.teswiz.services;

import com.znsio.teswiz.api.ApiEngine;
import com.znsio.teswiz.api.ApiEngineClient;
import com.znsio.teswiz.api.PlaywrightApiEngineClient;
import com.znsio.teswiz.api.RestAssuredApiEngineClient;
import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.tools.OverriddenVariable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class ApiService {
    private static final Logger LOGGER = LogManager.getLogger(ApiService.class);

    private ApiService() {
    }

    private static ApiEngineClient getClient() {
        String configuredEngine = OverriddenVariable.getOverriddenStringValue(Setup.API_ENGINE, ApiEngine.REST_ASSURED.getConfigValue());
        ApiEngine engine = ApiEngine.from(configuredEngine);
        LOGGER.debug("Selected API engine: {}", engine.getConfigValue());

        if (engine == ApiEngine.PLAYWRIGHT_JAVA) {
            return new PlaywrightApiEngineClient();
        }
        return new RestAssuredApiEngineClient();
    }

    public static TeswizApiResponse get(String url) {
        return get(url, null, null);
    }

    public static TeswizApiResponse get(String url, Map<String, Object> queryParams) {
        return get(url, queryParams, null);
    }

    public static TeswizApiResponse get(String url, Map<String, Object> queryParams, Map<String, String> headers) {
        return getClient().get(url, queryParams, headers);
    }

    public static TeswizApiResponse post(String url, Object body) {
        return post(url, body, null);
    }

    public static TeswizApiResponse post(String url, Object body, Map<String, String> headers) {
        return getClient().post(url, body, headers);
    }

    public static TeswizApiResponse patch(String url, Object body) {
        return patch(url, body, null);
    }

    public static TeswizApiResponse patch(String url, Object body, Map<String, String> headers) {
        return getClient().patch(url, body, headers);
    }

    public static TeswizApiResponse delete(String url) {
        return delete(url, null);
    }

    public static TeswizApiResponse delete(String url, Map<String, String> headers) {
        return getClient().delete(url, headers);
    }
}
