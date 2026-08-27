package com.znsio.teswiz.tools;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {
    private static final String MASK = "***";
    private static volatile boolean showSensitiveData = false;

    private static final List<String> DEFAULT_SENSITIVE_KEYS = List.of(
            "access[_-]?key", "api[_-]?key", "auth[_-]?token", "token", "password", "passwd", "secret",
            "client[_-]?secret", "cloud[_-]?key", "cloud[_-]?username", "pcloudy_apikey",
            "pcloudy_username", "authorization", "userName");

    private static final Pattern URL_CREDENTIALS = Pattern.compile(
            "(?i)(https?://)([^:/\\s]+):([^@\\s]+)@");
    private static final Pattern CURL_USER_CREDENTIALS = Pattern.compile(
            "(?i)(-u\\s+['\"]?)([^:\\s'\"\\\\]+):([^\\s'\"\\\\]+)(['\"]?)");
    private static final Pattern AUTHORIZATION_BEARER = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*bearer\\s+)([^,\\s]+)");

    private static volatile Pattern jsonSensitiveKeyValue = buildJsonKeyValuePattern(DEFAULT_SENSITIVE_KEYS);
    private static volatile Pattern textSensitiveKeyValue = buildTextKeyValuePattern(DEFAULT_SENSITIVE_KEYS);

    private SensitiveDataMasker() {}

    public static void setShowSensitiveData(boolean showSensitiveDataInLogs) {
        showSensitiveData = showSensitiveDataInLogs;
    }

    /**
     * Reconfigures which key names are treated as sensitive when masking key/value pairs
     * found in JSON and plain text (e.g. {@code "password": "..."} or {@code password=...}).
     * <p>
     * {@code overrideKeys} (when non-empty) replaces the built-in default key list entirely.
     * Otherwise, {@code additionalKeys} (when non-empty) is merged with the built-in defaults.
     * Keys are matched case-insensitively as whole words; they are treated as literal names,
     * not regular expressions.
     */
    public static void configureSensitiveKeys(List<String> additionalKeys, List<String> overrideKeys) {
        List<String> effectiveKeys = (null != overrideKeys && !overrideKeys.isEmpty())
                ? literalKeys(overrideKeys)
                : mergeWithDefaults(additionalKeys);
        jsonSensitiveKeyValue = buildJsonKeyValuePattern(effectiveKeys);
        textSensitiveKeyValue = buildTextKeyValuePattern(effectiveKeys);
    }

    public static void resetSensitiveKeysToDefault() {
        jsonSensitiveKeyValue = buildJsonKeyValuePattern(DEFAULT_SENSITIVE_KEYS);
        textSensitiveKeyValue = buildTextKeyValuePattern(DEFAULT_SENSITIVE_KEYS);
    }

    private static List<String> mergeWithDefaults(List<String> additionalKeys) {
        if (null == additionalKeys || additionalKeys.isEmpty()) {
            return DEFAULT_SENSITIVE_KEYS;
        }
        Set<String> merged = new LinkedHashSet<>(DEFAULT_SENSITIVE_KEYS);
        merged.addAll(literalKeys(additionalKeys));
        return List.copyOf(merged);
    }

    private static List<String> literalKeys(List<String> keys) {
        return keys.stream()
                .map(String::trim)
                .filter(key -> !key.isEmpty())
                .map(Pattern::quote)
                .toList();
    }

    private static Pattern buildJsonKeyValuePattern(List<String> keys) {
        return Pattern.compile(
                "(?i)(\"(?:" + String.join("|", keys) + ")\"\\s*:\\s*\")([^\"]+)(\")");
    }

    private static Pattern buildTextKeyValuePattern(List<String> keys) {
        // Boundaries use (?<![A-Za-z0-9])/(?![A-Za-z0-9]) rather than \b so that keys joined
        // by '_' or '-' (e.g. player_auth_token, session_token) are still matched - \b treats
        // '_' as a word character, so it fails to find a boundary at an underscore junction.
        return Pattern.compile(
                "(?i)(?<![A-Za-z0-9])(" + String.join("|", keys) + ")(?![A-Za-z0-9])"
                + "\\s*[:=]\\s*([^,&;\\s}\\]]+)");
    }

    public static String maskSecret(String value) {
        if (showSensitiveData) {
            return value;
        }
        if (value == null) {
            return value;
        }
        if (value.isBlank()) {
            return value;
        }
        return MASK;
    }

    public static String mask(String value) {
        if (showSensitiveData) {
            return value;
        }
        if (value == null) {
            return value;
        }
        if (value.isBlank()) {
            return value;
        }

        String masked = value;
        masked = URL_CREDENTIALS.matcher(masked).replaceAll("$1" + MASK + ":" + MASK + "@");
        masked = CURL_USER_CREDENTIALS.matcher(masked).replaceAll("$1" + MASK + ":" + MASK + "$4");
        masked = AUTHORIZATION_BEARER.matcher(masked).replaceAll("$1" + MASK);
        masked = jsonSensitiveKeyValue.matcher(masked).replaceAll("$1" + MASK + "$3");
        masked = textSensitiveKeyValue.matcher(masked).replaceAll("$1=" + MASK);
        return masked;
    }
}
