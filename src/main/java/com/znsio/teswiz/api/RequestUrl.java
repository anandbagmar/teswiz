package com.znsio.teswiz.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Prepares a request URL for an engine that must send it exactly as given (RestAssured with URL encoding turned off),
 * so it behaves like the Playwright engine, which follows the browser URL rules.
 *
 * <p>A URL a caller passes is treated as already encoded: a valid {@code %XX} escape is kept as it is, never encoded a
 * second time. Only characters that are not allowed in a URL at all (a space, a non-ASCII character, a stray {@code %})
 * are percent-encoded. Query-param values supplied as a map are encoded exactly once and appended.
 */
final class RequestUrl {

    private static final String ALLOWED_AS_IS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
            + "-._~:/?#[]@!$&'()*+,;=";
    private static final String HEX_DIGITS = "0123456789ABCDEFabcdef";

    private RequestUrl() {
    }

    /**
     * @param url a URL, normally already encoded
     * @return the URL with only the characters that are illegal in a URL percent-encoded (UTF-8)
     */
    static String encodeIllegalCharacters(String url) {
        if (url == null) {
            return null;
        }
        StringBuilder encoded = new StringBuilder(url.length());
        for (int index = 0; index < url.length(); index++) {
            char current = url.charAt(index);
            if (ALLOWED_AS_IS.indexOf(current) >= 0 || isValidEscapeAt(url, index)) {
                encoded.append(current);
            } else {
                encoded.append(percentEncode(url, index));
                index += Character.charCount(url.codePointAt(index)) - 1;
            }
        }
        return encoded.toString();
    }

    /**
     * @param url         the base URL, which may already carry a query
     * @param queryParams name/value pairs to append; null values are skipped (as the Playwright engine does)
     * @return the URL with each pair appended once-encoded, or the URL unchanged when there is nothing to add
     */
    static String withQueryParams(String url, Map<String, Object> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return url;
        }
        StringJoiner query = new StringJoiner("&");
        queryParams.forEach((name, value) -> {
            if (value != null) {
                query.add(encodeQueryPart(name) + "=" + encodeQueryPart(String.valueOf(value)));
            }
        });
        if (query.length() == 0) {
            return url;
        }
        return url + querySeparatorFor(url) + query;
    }

    private static boolean isValidEscapeAt(String url, int index) {
        return url.charAt(index) == '%' && index + 2 < url.length()
                && HEX_DIGITS.indexOf(url.charAt(index + 1)) >= 0 && HEX_DIGITS.indexOf(url.charAt(index + 2)) >= 0;
    }

    private static String percentEncode(String url, int index) {
        String character = new String(Character.toChars(url.codePointAt(index)));
        StringBuilder escaped = new StringBuilder();
        for (byte value : character.getBytes(StandardCharsets.UTF_8)) {
            escaped.append('%').append(String.format("%02X", value & 0xFF));
        }
        return escaped.toString();
    }

    /** Encodes a query name or value once; a space becomes {@code %20}, as RestAssured itself sent it. */
    private static String encodeQueryPart(String part) {
        return URLEncoder.encode(part, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String querySeparatorFor(String url) {
        if (!url.contains("?")) {
            return "?";
        }
        return url.endsWith("?") || url.endsWith("&") ? "" : "&";
    }
}
