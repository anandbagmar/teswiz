package com.znsio.teswiz.filters.apitraffic;

/**
 * Builds the per-call log file name in the form {@code NN-METHOD-sanitized-endpoint.log}.
 * <p>
 * Example: {@code 03-POST-api-v1-wallet-player-alice-authenticate.log}
 */
public final class ApiCallFileNamer {

    private static final int MAX_ENDPOINT_LENGTH = 80;

    private ApiCallFileNamer() {
    }

    /**
     * Builds the file name for a single API call.
     *
     * @param index    1-based call index within the scenario
     * @param method   HTTP method (any case)
     * @param endpoint request endpoint, optionally with a leading slash and query string
     * @return file name such as {@code 01-POST-api-v1-balance.log}
     */
    public static String fileName(int index, String method, String endpoint) {
        String paddedIndex = String.format("%02d", index);
        String upperMethod = method.toUpperCase();
        String sanitizedEndpoint = sanitizeEndpoint(endpoint);
        return paddedIndex + "-" + upperMethod + "-" + sanitizedEndpoint + ".log";
    }

    private static String sanitizeEndpoint(String endpoint) {
        String withoutQuery = stripQueryString(endpoint);
        String withoutLeadingSlash = withoutQuery.startsWith("/") ? withoutQuery.substring(1) : withoutQuery;
        String dashed = withoutLeadingSlash.replaceAll("[^A-Za-z0-9]+", "-");
        String trimmed = trimDashes(dashed);
        return capLength(trimmed);
    }

    private static String stripQueryString(String endpoint) {
        int queryStart = endpoint.indexOf('?');
        return queryStart >= 0 ? endpoint.substring(0, queryStart) : endpoint;
    }

    private static String trimDashes(String value) {
        return value.replaceAll("^-+", "").replaceAll("-+$", "");
    }

    private static String capLength(String value) {
        return value.length() > MAX_ENDPOINT_LENGTH ? value.substring(0, MAX_ENDPOINT_LENGTH) : value;
    }
}
