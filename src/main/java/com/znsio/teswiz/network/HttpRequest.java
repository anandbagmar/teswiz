package com.znsio.teswiz.network;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An HTTP request for {@link FallbackHttpClient}. Redirects are not followed unless asked, the same default as the JDK
 * {@code HttpClient} the callers used before.
 *
 * @param method          {@code GET}, {@code POST}, ...
 * @param url             the full URL
 * @param headers         request headers
 * @param body            the request body, or {@code null} for none
 * @param contentType     the body's content type, or {@code null} for none
 * @param timeout         how long the whole call may take
 * @param followRedirects whether to follow redirects
 */
public record HttpRequest(String method, String url, Map<String, String> headers, byte[] body, String contentType,
        Duration timeout, boolean followRedirects) {

    private static final String GET = "GET";
    private static final String POST = "POST";

    public HttpRequest {
        headers = Map.copyOf(headers);
    }

    /** @return a GET of {@code url} that may take up to {@code timeout} */
    public static HttpRequest get(String url, Duration timeout) {
        return new HttpRequest(GET, url, Map.of(), null, null, timeout, false);
    }

    /** @return a POST of {@code body} to {@code url} that may take up to {@code timeout} */
    public static HttpRequest post(String url, String contentType, byte[] body, Duration timeout) {
        return new HttpRequest(POST, url, Map.of(), body, contentType, timeout, false);
    }

    /** @return this request with one more header */
    public HttpRequest withHeader(String name, String value) {
        Map<String, String> withHeader = new LinkedHashMap<>(headers);
        withHeader.put(name, value);
        return new HttpRequest(method, url, withHeader, body, contentType, timeout, followRedirects);
    }

    /** @return this request, following redirects */
    public HttpRequest followingRedirects() {
        return new HttpRequest(method, url, headers, body, contentType, timeout, true);
    }
}
