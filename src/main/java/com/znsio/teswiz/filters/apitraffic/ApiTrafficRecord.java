package com.znsio.teswiz.filters.apitraffic;

/**
 * Immutable snapshot of a single API call's request and response, captured by the
 * {@link ApiTrafficLoggingFilter} and handed to {@link ApiTrafficRecorder} for logging.
 *
 * @param method          HTTP method (e.g. POST, GET, PUT)
 * @param endpoint        request endpoint, optionally with a query string
 * @param requestHeaders  request headers as text
 * @param requestBody     request body as text (may be empty)
 * @param statusCode      HTTP response status code
 * @param responseHeaders response headers as text
 * @param responseBody    response body as text
 */
public record ApiTrafficRecord(
        String method,
        String endpoint,
        String requestHeaders,
        String requestBody,
        int statusCode,
        String responseHeaders,
        String responseBody) {
}
