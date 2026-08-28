package com.znsio.teswiz.filters.apitraffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.tools.SensitiveDataMasker;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * RestAssured filter that captures every API call's request and response and
 * records them via {@link ApiTrafficRecorder} — one masked file per call under
 * the current scenario's report folder.
 * <p>
 * teswiz registers this filter globally when {@code API_TRAFFIC_LOGGING=true}
 * (see {@link ApiTrafficLogging}), so it covers all API clients without changing
 * how each client builds its request. For each call it emits a single console
 * reference line pointing to the detailed traffic file.
 * <p>
 * Recording happens in a {@code finally} block so a call is still logged even
 * when a downstream filter (e.g. {@code EnvironmentIssueFilter}) throws instead
 * of returning a response — those are exactly the calls worth having a record of.
 */
public class ApiTrafficLoggingFilter implements Filter {

    private static final Logger LOGGER = LogManager.getLogger(ApiTrafficLoggingFilter.class.getName());

    private final ApiTrafficRecorder recorder;

    public ApiTrafficLoggingFilter(ApiTrafficRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
            FilterableResponseSpecification responseSpec,
            FilterContext ctx) {
        Response response = null;
        try {
            response = ctx.next(requestSpec, responseSpec);
            return response;
        } finally {
            recordSafely(requestSpec, response);
        }
    }

    // --- Private helper methods ---

    private void recordSafely(FilterableRequestSpecification requestSpec, Response response) {
        try {
            ApiTrafficRecord record = toRecord(requestSpec, response);
            String relativePath = recorder.record(record);
            LOGGER.info("API call {} {} -> {} | detail: {}",
                    record.method(), SensitiveDataMasker.mask(record.endpoint()), record.statusCode(), relativePath);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to record api-traffic for {} {}: {}",
                    requestSpec.getMethod(), requestSpec.getURI(), e.getMessage());
        }
    }

    private ApiTrafficRecord toRecord(FilterableRequestSpecification requestSpec, Response response) {
        return response == null
                ? new ApiTrafficRecord(
                        requestSpec.getMethod(),
                        requestSpec.getURI(),
                        String.valueOf(requestSpec.getHeaders()),
                        bodyAsText(requestSpec.getBody()),
                        -1,
                        "",
                        "(no response — call failed before a response was received)")
                : new ApiTrafficRecord(
                        requestSpec.getMethod(),
                        requestSpec.getURI(),
                        String.valueOf(requestSpec.getHeaders()),
                        bodyAsText(requestSpec.getBody()),
                        response.getStatusCode(),
                        String.valueOf(response.getHeaders()),
                        response.getBody().asString());
    }

    private String bodyAsText(Object body) {
        return body == null ? "" : String.valueOf(body);
    }
}
