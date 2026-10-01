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
        int timeoutMs = OverriddenVariable.getOverriddenIntValue("TESWIZ_API_TIMEOUT_SECONDS", 60) * 1000;
        options.setTimeout(timeoutMs);

        Map<String, String> mergedHeaders = new HashMap<>();
        mergedHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        mergedHeaders.put("Accept", "application/json, text/html, */*");
        mergedHeaders.put("Connection", "close");
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

    private void applyQueryParams(RequestOptions options, Map<String, Object> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            if (entry.getValue() != null) {
                options.setQueryParam(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
    }

    private String applyRequestBody(RequestOptions options, Object body) {
        if (body == null) {
            return "";
        }
        if (body instanceof byte[]) {
            options.setData((byte[]) body);
            return "[binary data]";
        }
        if (body instanceof String) {
            options.setData((String) body);
            return (String) body;
        }
        if (body instanceof Map) {
            String jsonStr = new org.json.JSONObject((Map<?, ?>) body).toString();
            options.setData(jsonStr);
            return jsonStr;
        }
        if (body instanceof java.util.Collection) {
            String jsonStr = new org.json.JSONArray((java.util.Collection<?>) body).toString();
            options.setData(jsonStr);
            return jsonStr;
        }
        if (body instanceof org.json.JSONObject || body instanceof org.json.JSONArray) {
            String jsonStr = body.toString();
            options.setData(jsonStr);
            return jsonStr;
        }
        String strBody = body.toString();
        options.setData(strBody);
        return strBody;
    }

    private APIResponse dispatchHttpMethod(String method, String url, RequestOptions options) {
        return switch (method.toUpperCase()) {
            case "GET" -> getRequestContext().get(url, options);
            case "POST" -> getRequestContext().post(url, options);
            case "PUT" -> getRequestContext().put(url, options);
            case "PATCH" -> getRequestContext().patch(url, options);
            case "DELETE" -> getRequestContext().delete(url, options);
            case "HEAD" -> getRequestContext().fetch(url, options.setMethod("HEAD"));
            case "OPTIONS" -> getRequestContext().fetch(url, options.setMethod("OPTIONS"));
            default -> getRequestContext().fetch(url, options.setMethod(method));
        };
    }

    private TeswizApiResponse executeRequest(String method, String url, Object body, Map<String, Object> queryParams, Map<String, String> headers) {
        LOGGER.info("Processing {} call via Playwright API Engine", method);
        RequestOptions options = createRequestOptions(headers, body != null);
        String finalUrl = stripTrailingQuestionMark(url);
        applyQueryParams(options, queryParams);
        String requestBodyStr = applyRequestBody(options, body);

        int maxRetries = OverriddenVariable.getOverriddenIntValue("API_MAX_RETRIES", 3);
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            long startTime = System.currentTimeMillis();
            try {
                APIResponse response = dispatchHttpMethod(method, finalUrl, options);
                long responseTime = System.currentTimeMillis() - startTime;

                int statusCode = response.status();
                String responseBody = response.text();
                byte[] responseBytes = response.body();
                Map<String, String> responseHeaders = response.headers();

                TeswizApiResponse teswizApiResponse = new TeswizApiResponse(statusCode, responseBody, responseBytes, responseHeaders, responseTime);

                String attemptLabel = maxRetries > 1 ? String.format(" [Attempt %d/%d]", attempt, maxRetries) : "";
                String trafficFile = recordTrafficSafely(method, finalUrl + attemptLabel, headers != null ? headers.toString() : "{}", requestBodyStr, statusCode, responseHeaders.toString(), responseBody);

                checkEnvironmentIssue(finalUrl, statusCode, responseBody);

                if (attempt > 1) {
                    LOGGER.info("API request {} {} succeeded on retry attempt {}/{} | Recorded API traffic file: {}",
                            method, finalUrl, attempt, maxRetries, trafficFile);
                }

                return teswizApiResponse;

            } catch (EnvironmentSetupException e) {
                lastException = e;
                handleRetryOrThrow(method, finalUrl, attempt, maxRetries, headers, requestBodyStr, e);
            } catch (Exception e) {
                lastException = e;
                handleRetryOrThrow(method, finalUrl, attempt, maxRetries, headers, requestBodyStr, e);
            }
        }
        throw new RuntimeException("Playwright API request failed for " + method + " " + finalUrl + ": " + (lastException != null ? lastException.getMessage() : "unknown error"), lastException);
    }

    private void handleRetryOrThrow(String method, String url, int attempt, int maxRetries, Map<String, String> headers, String requestBodyStr, Exception e) {
        String attemptLabel = String.format(" [Attempt %d/%d FAILED: %s]", attempt, maxRetries, e.getMessage());
        String trafficFile = recordTrafficSafely(method, url + attemptLabel, headers != null ? headers.toString() : "{}", requestBodyStr, -1, "{}", "(call failed on attempt " + attempt + "/" + maxRetries + ": " + e.getMessage() + ")");
        String trafficFileInfo = (trafficFile != null && !trafficFile.isEmpty()) ? " | Recorded API traffic file: " + trafficFile : "";

        if (attempt < maxRetries) {
            long delayMs = attempt * 1000L;
            LOGGER.warn("API request {} {} failed on attempt {}/{} ({}){} | Retrying attempt {}/{} in {} ms...",
                    method, url, attempt, maxRetries, e.getMessage(), trafficFileInfo, attempt + 1, maxRetries, delayMs);
            sleepSafely(delayMs);
        } else if (e instanceof EnvironmentSetupException) {
            LOGGER.error("API request {} {} failed after {} attempts due to environment issue{} | Cause: {}",
                    method, url, maxRetries, trafficFileInfo, e.getMessage());
            throw (EnvironmentSetupException) e;
        } else {
            LOGGER.error("API request {} {} failed after {} attempts{} | Cause: {}",
                    method, url, maxRetries, trafficFileInfo, e.getMessage());
            throw new RuntimeException("Playwright API request failed for " + method + " " + url + " after " + maxRetries + " attempts" + trafficFileInfo + ": " + e.getMessage(), e);
        }
    }

    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    private String recordTrafficSafely(String method, String endpoint, String reqHeaders, String reqBody, int statusCode, String respHeaders, String respBody) {
        if (!ApiTrafficLogging.isEnabled()) {
            return "";
        }
        try {
            ApiTrafficRecorder recorder = new ApiTrafficRecorder(new TeswizScenarioDirectoryResolver());
            ApiTrafficRecord record = new ApiTrafficRecord(method, endpoint, reqHeaders, reqBody, statusCode, respHeaders, respBody);
            String relativePath = recorder.record(record);
            LOGGER.info("API call {} {} -> {} | detail: {}",
                    record.method(), SensitiveDataMasker.mask(record.endpoint()), record.statusCode(), relativePath);
            return relativePath;
        } catch (Exception e) {
            LOGGER.warn("Failed to record api-traffic for {} {}: {}", method, endpoint, e.getMessage());
            return "";
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
