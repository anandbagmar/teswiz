package com.znsio.teswiz.runner;

import java.util.HashMap;
import java.util.Map;

/**
 * Injectable holder for the three parallel configuration maps that {@link Setup} previously owned
 * as raw static fields: string, boolean, and integer values keyed by name.
 *
 * <p>It exposes safe, typed accessors - notably {@link #getInteger(String)} returns {@code 0} for a
 * missing key instead of throwing a {@link NullPointerException} on unboxing. {@link Setup} keeps
 * its static facade and delegates to a single instance of this class, so existing callers are
 * unaffected while configuration becomes an injectable, independently testable collaborator.
 *
 * <p>The backing maps are exposed via {@link #strings()}, {@link #booleans()}, and
 * {@link #integers()} so {@link Setup}'s existing internal map references continue to work during
 * the incremental extraction.
 */
public class TeswizConfiguration {

    private final Map<String, String> strings = new HashMap<>();
    private final Map<String, Boolean> booleans = new HashMap<>();
    private final Map<String, Integer> integers = new HashMap<>();

    public String getString(String key) {
        return strings.get(key);
    }

    public String getStringOrDefault(String key, String defaultValue) {
        return strings.getOrDefault(key, defaultValue);
    }

    public void putString(String key, String value) {
        strings.put(key, value);
    }

    public boolean getBoolean(String key) {
        return Boolean.TRUE.equals(booleans.get(key));
    }

    public String getBooleanAsString(String key) {
        return String.valueOf(booleans.get(key));
    }

    public void putBoolean(String key, boolean value) {
        booleans.put(key, value);
    }

    /**
     * Returns the integer value for the key, or {@code 0} when absent. Returning a safe default
     * avoids the {@link NullPointerException} that unboxing a missing key would otherwise throw.
     */
    public int getInteger(String key) {
        return integers.getOrDefault(key, 0);
    }

    public String getIntegerAsString(String key) {
        return String.valueOf(integers.getOrDefault(key, 0));
    }

    public void putInteger(String key, Integer value) {
        integers.put(key, value);
    }

    public void clear() {
        strings.clear();
        booleans.clear();
        integers.clear();
    }

    Map<String, String> strings() {
        return strings;
    }

    Map<String, Boolean> booleans() {
        return booleans;
    }

    Map<String, Integer> integers() {
        return integers;
    }
}
