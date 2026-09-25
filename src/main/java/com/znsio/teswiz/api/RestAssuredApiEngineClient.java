package com.znsio.teswiz.api;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
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

    private TeswizApiResponse convertResponse(Response response, long responseTimeInMs) {
        Map<String, String> headersMap = new HashMap<>();
        for (Header header : response.getHeaders()) {
            headersMap.put(header.getName(), header.getValue());
        }
        return new TeswizApiResponse(
                response.getStatusCode(),
                response.getBody() != null ? response.getBody().asString() : "",
                response.getBody() != null ? response.getBody().asByteArray() : new byte[0],
                headersMap,
                responseTimeInMs
        );
    }

    @Override
    public TeswizApiResponse get(String url, Map<String, Object> queryParams, Map<String, String> headers) {
        LOGGER.info("Processing GET call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        if (queryParams != null && !queryParams.isEmpty()) {
            requestSpec.queryParams(queryParams);
        }
        long startTime = System.currentTimeMillis();
        Response response = requestSpec.get(stripTrailingQuestionMark(url));
        long responseTime = System.currentTimeMillis() - startTime;
        return convertResponse(response, responseTime);
    }

    @Override
    public TeswizApiResponse post(String url, Object body, Map<String, String> headers) {
        LOGGER.info("Processing POST call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        if (body != null) {
            requestSpec.body(body);
        }
        long startTime = System.currentTimeMillis();
        Response response = requestSpec.post(url);
        long responseTime = System.currentTimeMillis() - startTime;
        return convertResponse(response, responseTime);
    }

    @Override
    public TeswizApiResponse postMultipart(String url, Map<String, Object> formFields, Map<String, File> files, Map<String, String> headers) {
        LOGGER.info("Processing POST multipart call via RestAssured");
        RequestSpecification requestSpec = RestAssured.given().relaxedHTTPSValidation();
        if (headers != null && !headers.isEmpty()) {
            requestSpec.headers(headers);
        }
        if (formFields != null && !formFields.isEmpty()) {
            for (Map.Entry<String, Object> entry : formFields.entrySet()) {
                requestSpec.formParam(entry.getKey(), entry.getValue());
            }
        }
        if (files != null && !files.isEmpty()) {
            for (Map.Entry<String, File> entry : files.entrySet()) {
                requestSpec.multiPart(entry.getKey(), entry.getValue());
            }
        }
        long startTime = System.currentTimeMillis();
        Response response = requestSpec.post(url);
        long responseTime = System.currentTimeMillis() - startTime;
        return convertResponse(response, responseTime);
    }

    @Override
    public TeswizApiResponse put(String url, Object body, Map<String, String> headers) {
        LOGGER.info("Processing PUT call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        if (body != null) {
            requestSpec.body(body);
        }
        long startTime = System.currentTimeMillis();
        Response response = requestSpec.put(url);
        long responseTime = System.currentTimeMillis() - startTime;
        return convertResponse(response, responseTime);
    }

    @Override
    public TeswizApiResponse patch(String url, Object body, Map<String, String> headers) {
        LOGGER.info("Processing PATCH call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        if (body != null) {
            requestSpec.body(body);
        }
        long startTime = System.currentTimeMillis();
        Response response = requestSpec.patch(url);
        long responseTime = System.currentTimeMillis() - startTime;
        return convertResponse(response, responseTime);
    }

    @Override
    public TeswizApiResponse delete(String url, Map<String, String> headers) {
        LOGGER.info("Processing DELETE call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        long startTime = System.currentTimeMillis();
        Response response = requestSpec.delete(url);
        long responseTime = System.currentTimeMillis() - startTime;
        return convertResponse(response, responseTime);
    }

    @Override
    public TeswizApiResponse head(String url, Map<String, String> headers) {
        LOGGER.info("Processing HEAD call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        long startTime = System.currentTimeMillis();
        Response response = requestSpec.head(url);
        long responseTime = System.currentTimeMillis() - startTime;
        return convertResponse(response, responseTime);
    }

    @Override
    public TeswizApiResponse options(String url, Map<String, String> headers) {
        LOGGER.info("Processing OPTIONS call via RestAssured");
        RequestSpecification requestSpec = getRequestSpec(headers);
        long startTime = System.currentTimeMillis();
        Response response = requestSpec.options(url);
        long responseTime = System.currentTimeMillis() - startTime;
        return convertResponse(response, responseTime);
    }
}
