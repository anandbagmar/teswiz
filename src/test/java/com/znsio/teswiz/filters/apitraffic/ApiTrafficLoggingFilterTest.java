package com.znsio.teswiz.filters.apitraffic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.restassured.filter.FilterContext;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Unit tests for {@link ApiTrafficLoggingFilter} — captures each RestAssured
 * call, passes the response through unchanged, and records a per-call file.
 */
class ApiTrafficLoggingFilterTest {

    @AfterEach
    void tearDown() {
        ApiCallContext.clear();
    }

    @Test
    void filter_passesResponseThroughUnchanged(@TempDir Path scenarioDir) {
        ApiTrafficLoggingFilter filter = filterWriting(scenarioDir);
        FilterableRequestSpecification requestSpec = requestSpec();
        Response response = response(200, "{\"ok\":true}");
        FilterContext ctx = ctxReturning(response);

        Response actual = filter.filter(requestSpec, mock(FilterableResponseSpecification.class), ctx);

        assertThat(actual).isSameAs(response);
    }

    @Test
    void filter_writesOnePerCallFileForTheRequest(@TempDir Path scenarioDir) {
        ApiTrafficLoggingFilter filter = filterWriting(scenarioDir);
        Response response = response(200, "{\"ok\":true}");

        filter.filter(requestSpec(), mock(FilterableResponseSpecification.class), ctxReturning(response));

        Path apiTraffic = scenarioDir.resolve("api-traffic");
        assertThat(Files.isDirectory(apiTraffic)).isTrue();
        assertThat(apiTraffic.toFile().listFiles()).hasSize(1);
    }

    @Test
    void filter_doesNotThrowWhenRecordingFails(@TempDir Path scenarioDir) {
        ApiTrafficRecorder throwingRecorder = new ApiTrafficRecorder(() -> {
            throw new IllegalStateException("resolver boom");
        });
        ApiTrafficLoggingFilter filter = new ApiTrafficLoggingFilter(throwingRecorder);
        Response response = response(200, "{\"ok\":true}");

        Response actual = filter.filter(requestSpec(), mock(FilterableResponseSpecification.class), ctxReturning(response));

        assertThat(actual).isSameAs(response);
    }

    @Test
    void filter_stillWritesTrafficFileWhenDownstreamFilterThrows(@TempDir Path scenarioDir) {
        ApiTrafficLoggingFilter filter = filterWriting(scenarioDir);
        FilterContext ctx = mock(FilterContext.class);
        when(ctx.next(any(), any())).thenThrow(new IllegalStateException("environment issue: HTTP 503"));

        assertThatThrownBy(() -> filter.filter(requestSpec(), mock(FilterableResponseSpecification.class), ctx))
                .isInstanceOf(IllegalStateException.class);

        Path apiTraffic = scenarioDir.resolve("api-traffic");
        assertThat(Files.isDirectory(apiTraffic)).isTrue();
        assertThat(apiTraffic.toFile().listFiles()).hasSize(1);
    }

    private ApiTrafficLoggingFilter filterWriting(Path scenarioDir) {
        return new ApiTrafficLoggingFilter(new ApiTrafficRecorder(() -> scenarioDir));
    }

    private FilterableRequestSpecification requestSpec() {
        FilterableRequestSpecification requestSpec = mock(FilterableRequestSpecification.class);
        when(requestSpec.getMethod()).thenReturn("POST");
        when(requestSpec.getURI()).thenReturn("https://example.com/api/v1/balance");
        when(requestSpec.getHeaders()).thenReturn(new Headers());
        when(requestSpec.getBody()).thenReturn("{\"amount\":10.0}");
        return requestSpec;
    }

    private Response response(int statusCode, String body) {
        Response response = mock(Response.class);
        ResponseBody<?> responseBody = mock(ResponseBody.class);
        when(response.getStatusCode()).thenReturn(statusCode);
        when(response.getHeaders()).thenReturn(new Headers());
        when(response.getBody()).thenReturn(responseBody);
        when(responseBody.asString()).thenReturn(body);
        return response;
    }

    private FilterContext ctxReturning(Response response) {
        FilterContext ctx = mock(FilterContext.class);
        when(ctx.next(any(), any())).thenReturn(response);
        return ctx;
    }
}
