package com.znsio.teswiz.api;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class RestAssuredApiEngineClient implements ApiEngineClient {
    private static final Logger LOGGER = LogManager.getLogger(RestAssuredApiEngineClient.class);

    private RequestSpecification getRequestSpec(Map<String, String> customHeaders) {
        Map<String, String> headers = getDefaultHeaders();
        if (customHeaders != null && !customHeaders.isEmpty()) {
            headers.putAll(customHeaders);
        }
        return RestAssured.given().relaxedHTTPSValidation().headers(headers);
    }

    private Map<String, String> getDefaultHeaders() {
        Map<String, String> defaultHeaders = new HashMap<>();
        defaultHeaders.put("Accept", "application/json, text/html, */*");
        defaultHeaders.put("content-type", "application/json");
        return defaultHeaders;
    }

    private String stripTrailingQuestionMark(String url) {
        return url != null && url.endsWith("?") ? url.substring(0, url.length() - 1) : url;
    }

    private TeswizApiResponse convertResponse(Response response) {
        Map<String, String> headersMap = new HashMap<>();
        for (Header header : response.getHeaders()) {
            headersMap.put(header.getName(), header.getValue());
        }
        return new TeswizApiResponse(
                response.getStatusCode(),
                response.getBody() != null ? response.getBody().asString() : "",
                response.getBody() != null ? response.getBody().asByteArray() : new byte[0],
                headersMap
        );
    }

    @Override
    public TeswizApiResponse get(String url, Map<String, Object> queryParams, Map<String, String> headers) {
        LOGGER.info("Processing GET call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        if (queryParams != null && !queryParams.isEmpty()) {
            requestSpec.queryParams(queryParams);
        }
        Response response = requestSpec.get(stripTrailingQuestionMark(url));
        return convertResponse(response);
    }

    @Override
    public TeswizApiResponse post(String url, Object body, Map<String, String> headers) {
        LOGGER.info("Processing POST call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        if (body != null) {
            requestSpec.body(body);
        }
        Response response = requestSpec.post(url);
        return convertResponse(response);
    }

    @Override
    public TeswizApiResponse put(String url, Object body, Map<String, String> headers) {
        LOGGER.info("Processing PUT call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        if (body != null) {
            requestSpec.body(body);
        }
        Response response = requestSpec.put(url);
        return convertResponse(response);
    }

    @Override
    public TeswizApiResponse patch(String url, Object body, Map<String, String> headers) {
        LOGGER.info("Processing PATCH call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        if (body != null) {
            requestSpec.body(body);
        }
        Response response = requestSpec.patch(url);
        return convertResponse(response);
    }

    @Override
    public TeswizApiResponse delete(String url, Map<String, String> headers) {
        LOGGER.info("Processing DELETE call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        Response response = requestSpec.delete(url);
        return convertResponse(response);
    }

    @Override
    public TeswizApiResponse head(String url, Map<String, String> headers) {
        LOGGER.info("Processing HEAD call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        Response response = requestSpec.head(url);
        return convertResponse(response);
    }

    @Override
    public TeswizApiResponse options(String url, Map<String, String> headers) {
        LOGGER.info("Processing OPTIONS call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        Response response = requestSpec.options(url);
        return convertResponse(response);
    }
}
