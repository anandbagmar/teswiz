package com.znsio.teswiz.api;

import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Describes the IP addresses a request URL's host resolves to, so a request that timed out can be
 * diagnosed (for example, one blocked address among several). Never throws.
 */
final class HostAddressDescriber {
    private static final String UNKNOWN_HOST = "unknown host";

    private HostAddressDescriber() {
    }

    static String describe(String url) {
        String host = extractHost(url);
        if (host == null) {
            return UNKNOWN_HOST + " for url: " + url;
        }
        return host + " -> " + resolveAddresses(host);
    }

    private static String extractHost(String url) {
        try {
            return URI.create(url).getHost();
        } catch (IllegalArgumentException invalidUrl) {
            return null;
        }
    }

    private static String resolveAddresses(String host) {
        try {
            return Arrays.stream(InetAddress.getAllByName(host))
                    .map(InetAddress::getHostAddress)
                    .collect(Collectors.joining(", ", "[", "]"));
        } catch (Exception resolutionFailure) {
            return "unresolved (" + resolutionFailure.getClass().getSimpleName() + ")";
        }
    }
}
