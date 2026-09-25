package com.znsio.teswiz.api;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import com.znsio.teswiz.exceptions.EnvironmentSetupException;
import com.znsio.teswiz.filters.apitraffic.ApiTrafficLogging;
import com.znsio.teswiz.filters.apitraffic.ApiTrafficRecord;
import com.znsio.teswiz.filters.apitraffic.ApiTrafficRecorder;
import com.znsio.teswiz.filters.apitraffic.TeswizScenarioDirectoryResolver;
import com.znsio.teswiz.tools.OverriddenVariable;
import com.znsio.teswiz.tools.SensitiveDataMasker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PlaywrightApiEngineClient implements ApiEngineClient {
    private static final Logger LOGGER = LogManager.getLogger(PlaywrightApiEngineClient.class);
    private static final int HTTP_BAD_GATEWAY = 502;
    private static final int HTTP_SERVICE_UNAVAILABLE = 503;
    private static final int HTTP_GATEWAY_TIMEOUT = 504;

    private APIRequestContext getRequestContext() {
        return PlaywrightApiManager.getAPIRequestContext();
    }

    private RequestOptions createRequestOptions(Map<String, String> headers, boolean hasBody) {
        RequestOptions options = RequestOptions.create();
        Map<String, String> mergedHeaders = new HashMap<>();
        mergedHeaders.put("Accept", "application/json, text/html, */*");
        if (hasBody) {
            mergedHeaders.put("content-type", "application/json");
        }
        if (headers != null && !headers.isEmpty()) {
            mergedHeaders.putAll(headers);
        }
        for (Map.Entry<String, String> entry : mergedHeaders.entrySet()) {
            options.setHeader(entry.getKey(), entry.getValue());
        }
        return options;
    }

    private String stripTrailingQuestionMark(String url) {
        return url != null && url.endsWith("?") ? url.substring(0, url.length() - 1) : url;
    }

    private TeswizApiResponse executeRequest(String method, String url, Object body, Map<String, Object> queryParams, Map<String, String> headers) {
        LOGGER.info("Processing {} call via Playwright API Engine", method);
        RequestOptions options = createRequestOptions(headers, body != null);
        String finalUrl = stripTrailingQuestionMark(url);

        if (queryParams != null && !queryParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                if (entry.getValue() != null) {
                    options.setQueryParam(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }

        String requestBodyStr = "";
        if (body != null) {
            if (body instanceof byte[]) {
                options.setData((byte[]) body);
                requestBodyStr = "[binary data]";
            } else if (body instanceof String) {
                options.setData((String) body);
                requestBodyStr = (String) body;
            } else if (body instanceof Map) {
                String jsonStr = new org.json.JSONObject((Map<?, ?>) body).toString();
                options.setData(jsonStr);
                requestBodyStr = jsonStr;
            } else if (body instanceof java.util.Collection) {
                String jsonStr = new org.json.JSONArray((java.util.Collection<?>) body).toString();
                options.setData(jsonStr);
                requestBodyStr = jsonStr;
            } else if (body instanceof org.json.JSONObject || body instanceof org.json.JSONArray) {
                String jsonStr = body.toString();
                options.setData(jsonStr);
                requestBodyStr = jsonStr;
            } else {
                String strBody = body.toString();
                options.setData(strBody);
                requestBodyStr = strBody;
            }
        }

        APIResponse response = null;
        long startTime = System.currentTimeMillis();
        try {
            switch (method.toUpperCase()) {
                case "GET":
                    response = getRequestContext().get(finalUrl, options);
                    break;
                case "POST":
                    response = getRequestContext().post(finalUrl, options);
                    break;
                case "PUT":
                    response = getRequestContext().put(finalUrl, options);
                    break;
                case "PATCH":
                    response = getRequestContext().patch(finalUrl, options);
                    break;
                case "DELETE":
                    response = getRequestContext().delete(finalUrl, options);
                    break;
                case "HEAD":
                    response = getRequestContext().fetch(finalUrl, options.setMethod("HEAD"));
                    break;
                case "OPTIONS":
                    response = getRequestContext().fetch(finalUrl, options.setMethod("OPTIONS"));
                    break;
                default:
                    response = getRequestContext().fetch(finalUrl, options.setMethod(method));
                    break;
            }
            long responseTime = System.currentTimeMillis() - startTime;
            int statusCode = response.status();
            String responseBody = response.text();
            byte[] responseBytes = response.body();
            Map<String, String> responseHeaders = response.headers();

            TeswizApiResponse teswizApiResponse = new TeswizApiResponse(statusCode, responseBody, responseBytes, responseHeaders, responseTime);

            recordTrafficSafely(method, finalUrl, headers != null ? headers.toString() : "{}", requestBodyStr, statusCode, responseHeaders.toString(), responseBody);
            checkEnvironmentIssue(finalUrl, statusCode, responseBody);

            return teswizApiResponse;

        } catch (EnvironmentSetupException e) {
            throw e;
        } catch (Exception e) {
            if (response != null) {
                recordTrafficSafely(method, finalUrl, headers != null ? headers.toString() : "{}", requestBodyStr, response.status(), response.headers().toString(), response.text());
            } else {
                recordTrafficSafely(method, finalUrl, headers != null ? headers.toString() : "{}", requestBodyStr, -1, "{}", "(no response — call failed: " + e.getMessage() + ")");
            }
            throw new RuntimeException("Playwright API request failed for " + method + " " + finalUrl + ": " + e.getMessage(), e);
        }
    }

    private void checkEnvironmentIssue(String url, int status, String body) {
        if (OverriddenVariable.getOverriddenBooleanValue("DISABLE_ENVIRONMENT_ISSUE_FILTER", false)) {
            return;
        }
        if (status == HTTP_BAD_GATEWAY || status == HTTP_SERVICE_UNAVAILABLE || status == HTTP_GATEWAY_TIMEOUT) {
            String truncatedBody = body != null && body.length() <= 200 ? body : (body != null ? body.substring(0, 200) + "..." : "");
            throw new EnvironmentSetupException(String.format(
                    "Environment issue: service at '%s' returned HTTP %d. " +
                            "This is not a test failure — the service is unavailable. Body: %s",
                    url, status, truncatedBody));
        }
    }

    private void recordTrafficSafely(String method, String endpoint, String reqHeaders, String reqBody, int statusCode, String respHeaders, String respBody) {
        if (!ApiTrafficLogging.isEnabled()) {
            return;
        }
        try {
            ApiTrafficRecorder recorder = new ApiTrafficRecorder(new TeswizScenarioDirectoryResolver());
            ApiTrafficRecord record = new ApiTrafficRecord(method, endpoint, reqHeaders, reqBody, statusCode, respHeaders, respBody);
            String relativePath = recorder.record(record);
            LOGGER.info("API call {} {} -> {} | detail: {}",
                    record.method(), SensitiveDataMasker.mask(record.endpoint()), record.statusCode(), relativePath);
        } catch (Exception e) {
            LOGGER.warn("Failed to record api-traffic for {} {}: {}", method, endpoint, e.getMessage());
        }
    }

    @Override
    public TeswizApiResponse get(String url, Map<String, Object> queryParams, Map<String, String> headers) {
        return executeRequest("GET", url, null, queryParams, headers);
    }

    @Override
    public TeswizApiResponse post(String url, Object body, Map<String, String> headers) {
        return executeRequest("POST", url, body, null, headers);
    }

    @Override
    public TeswizApiResponse postMultipart(String url, Map<String, Object> formFields, Map<String, File> files, Map<String, String> headers) {
        LOGGER.info("Processing POST multipart call via Playwright API Engine");
        RequestOptions options = RequestOptions.create();
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                options.setHeader(entry.getKey(), entry.getValue());
            }
        }
        FormData formData = FormData.create();
        if (formFields != null && !formFields.isEmpty()) {
            for (Map.Entry<String, Object> entry : formFields.entrySet()) {
                formData.set(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        if (files != null && !files.isEmpty()) {
            for (Map.Entry<String, File> entry : files.entrySet()) {
                formData.set(entry.getKey(), entry.getValue().toPath());
            }
        }
        options.setMultipart(formData);

        String finalUrl = stripTrailingQuestionMark(url);
        long startTime = System.currentTimeMillis();
        APIResponse response = getRequestContext().post(finalUrl, options);
        long responseTime = System.currentTimeMillis() - startTime;

        int statusCode = response.status();
        String responseBody = response.text();
        byte[] responseBytes = response.body();
        Map<String, String> responseHeaders = response.headers();

        TeswizApiResponse teswizApiResponse = new TeswizApiResponse(statusCode, responseBody, responseBytes, responseHeaders, responseTime);

        recordTrafficSafely("POST", finalUrl, headers != null ? headers.toString() : "{}", "[multipart data]", statusCode, responseHeaders.toString(), responseBody);
        checkEnvironmentIssue(finalUrl, statusCode, responseBody);

        return teswizApiResponse;
    }

    @Override
    public TeswizApiResponse put(String url, Object body, Map<String, String> headers) {
        return executeRequest("PUT", url, body, null, headers);
    }

    @Override
    public TeswizApiResponse patch(String url, Object body, Map<String, String> headers) {
        return executeRequest("PATCH", url, body, null, headers);
    }

    @Override
    public TeswizApiResponse delete(String url, Map<String, String> headers) {
        return executeRequest("DELETE", url, null, null, headers);
    }

    @Override
    public TeswizApiResponse head(String url, Map<String, String> headers) {
        return executeRequest("HEAD", url, null, null, headers);
    }

    @Override
    public TeswizApiResponse options(String url, Map<String, String> headers) {
        return executeRequest("OPTIONS", url, null, null, headers);
    }
}
