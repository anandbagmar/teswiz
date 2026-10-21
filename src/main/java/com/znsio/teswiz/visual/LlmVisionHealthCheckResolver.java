package com.znsio.teswiz.visual;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Pure decision logic for what the LLM vision startup health-check should probe, given the optional
 * {@code LLM_VISION_HEALTH_CHECK_URL} override and the provider's own derived probe. Kept free of
 * {@code Runner}/HTTP so it can be unit-tested in isolation; {@link LlmVisionConfigValidator} wires
 * it to configuration and performs the actual request.
 *
 * <p>Resolution rules (the override always wins over the derived probe):
 * <ul>
 *   <li><b>blank</b> - use the provider's derived probe (may itself be empty = structural-only);</li>
 *   <li><b>an opt-out literal</b> ({@code none}/{@code off}/{@code false}, case-insensitive) - skip
 *       the live probe entirely (structural validation only);</li>
 *   <li><b>an explicit http(s) URL</b> - probe that URL verbatim, reusing the provider's auth
 *       headers so only the target changes, not the credential shape.</li>
 * </ul>
 */
public final class LlmVisionHealthCheckResolver {

    private static final Set<String> OPT_OUT_VALUES = Set.of("none", "off", "false");

    private LlmVisionHealthCheckResolver() {
    }

    /**
     * @param overrideValue  the configured {@code LLM_VISION_HEALTH_CHECK_URL} (may be blank)
     * @param derivedProbe   the provider's own non-billable probe, if any
     * @param providerHeaders the provider's auth headers, reused when an explicit URL override wins
     * @return the probe to run, or empty for structural-only validation
     */
    public static Optional<LlmVisionHealthCheckProbe> resolve(String overrideValue,
                                                              Optional<LlmVisionHealthCheckProbe> derivedProbe,
                                                              Map<String, String> providerHeaders) {
        String override = overrideValue == null ? "" : overrideValue.trim();
        if (override.isEmpty()) {
            return derivedProbe;
        }
        if (isOptOut(override)) {
            return Optional.empty();
        }
        return Optional.of(new LlmVisionHealthCheckProbe(override, providerHeaders));
    }

    public static boolean isOptOut(String value) {
        return value != null && OPT_OUT_VALUES.contains(value.trim().toLowerCase());
    }

    public static boolean isWellFormedHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                   && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
