package com.znsio.teswiz.network;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The answer to an {@link HttpRequest}.
 *
 * @param status  the HTTP status code
 * @param body    the response body (empty when there is none)
 * @param headers the response headers
 */
public record HttpResponse(int status, byte[] body, Map<String, List<String>> headers) {

    public HttpResponse {
        headers = Map.copyOf(headers);
    }

    /** @return the body as UTF-8 text */
    public String bodyText() {
        return new String(body, StandardCharsets.UTF_8);
    }

    /** @return the first value of the header {@code name} (any case), when present */
    public Optional<String> header(String name) {
        return headers.entrySet().stream().filter(header -> header.getKey().equalsIgnoreCase(name))
                .flatMap(header -> header.getValue().stream()).findFirst();
    }
}
